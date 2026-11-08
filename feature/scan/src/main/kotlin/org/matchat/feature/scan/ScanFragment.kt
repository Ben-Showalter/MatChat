package org.matchat.feature.scan

import android.Manifest
import android.content.pm.PackageManager
import android.hardware.Camera
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.view.Surface
import android.view.SurfaceHolder
import android.view.View
import android.widget.FrameLayout
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import org.matchat.core.model.setup.ScanKind
import org.matchat.core.ui.focus.FocusEngine
import org.matchat.core.ui.menu.MenuItem
import org.matchat.core.ui.menu.MenuSheet
import org.matchat.core.ui.nav.Navigator
import org.matchat.core.ui.nav.ScanRequest
import org.matchat.core.ui.softkey.SoftkeyFragment
import org.matchat.core.ui.theme.themeColor
import org.matchat.feature.scan.databinding.FragmentScanBinding
import org.matchat.core.ui.R as UiR

/**
 * S31 Scan: MatChat's own camera screen (docs/adr/0010) — never a scanner app,
 * never ML Kit. The classic camera API (the one every target flip phone runs)
 * feeds preview frames to [QrDecoder] on a background thread, one at a time;
 * frames are never saved. What a decoded code *means* is [ScanViewModel]'s call;
 * this only drives the camera and renders. Ported from BusyCall's ScanActivity.
 * Reached from a menu or a row, never the camera key (the OS swallows it).
 */
@Suppress("DEPRECATION") // android.hardware.Camera: the one camera API every target phone has working.
@AndroidEntryPoint
class ScanFragment : SoftkeyFragment(), SurfaceHolder.Callback, Camera.PreviewCallback {

    override val contentLayoutId: Int = R.layout.fragment_scan
    override val leftLabel: CharSequence get() = getString(UiR.string.softkey_options)
    override val centerLabel: CharSequence get() = getString(UiR.string.softkey_select)

    private val viewModel: ScanViewModel by viewModels()
    private val navigator: Navigator get() = requireActivity() as Navigator
    private var binding: FragmentScanBinding? = null
    private val main = Handler(Looper.getMainLooper())

    private var camera: Camera? = null
    private var cameraId = 0
    private var previewWidth = 0
    private var previewHeight = 0
    private var rotated = false
    private var surfaceReady = false
    private var resumed = false
    private var decodeThread: HandlerThread? = null
    private var decodeHandler: Handler? = null

    @Volatile private var busy = false

    private val cameraPermission = registerForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) startCamera() else viewModel.onAction(ScanAction.PermissionDenied)
    }

    override fun onContentViewCreated(content: View) {
        val b = FragmentScanBinding.bind(content)
        binding = b
        setTitle(getString(R.string.scan_title))
        b.scanAgain.setOnClickListener { viewModel.onAction(ScanAction.ScanAgain) }
        b.scanPreview.holder.addCallback(this)
        // The flip or a rotation changes the area: keep the picture's shape.
        b.scanArea.addOnLayoutChangeListener { _, l, t, r, bottom, ol, ot, or_, ob ->
            if (r - l != or_ - ol || bottom - t != ob - ot) b.scanArea.post { sizePreview() }
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect(::render) }
                launch { viewModel.navEvents.collect(::onNav) }
            }
        }
        FocusEngine.requestInitialFocus(b.scanAgain)
    }

    private fun render(state: ScanState) {
        val b = binding ?: return
        b.scanStatus.setText(statusText(state.status))
        b.scanStatus.setTextColor(
            requireContext().themeColor(if (state.isProblem) UiR.attr.colorError else UiR.attr.colorTextPrimary),
        )
        b.scanAgain.setText(if (state.canRetry) R.string.scan_try_again else R.string.scan_again)
    }

    private fun statusText(status: ScanStatus): Int = when (status) {
        ScanStatus.STARTING -> R.string.scan_starting
        ScanStatus.POINT -> R.string.scan_point
        ScanStatus.NOT_A_SETUP_CODE -> R.string.scan_not_a_setup_code
        ScanStatus.NOT_A_RECOVERY_KEY -> R.string.scan_not_a_recovery_key
        ScanStatus.NOT_AN_ADDRESS -> R.string.scan_not_an_address
        ScanStatus.TOO_NEW -> R.string.scan_too_new
        ScanStatus.NO_CAMERA -> R.string.scan_no_camera
        ScanStatus.NO_PERMISSION -> R.string.scan_no_permission
    }

    private fun onNav(nav: ScanNav) {
        when (nav) {
            is ScanNav.Accepted -> {
                stopCamera()
                ScanRequest.deliver(this, kind(), nav.text)
                onBack()
            }
            ScanNav.RestartCamera -> ensureCameraPermissionThenStart()
        }
    }

    private fun kind(): ScanKind =
        requireArguments().getString("kind")?.let { name -> ScanKind.entries.firstOrNull { it.name == name } }
            ?: ScanKind.SETUP_CODE

    override fun onOptions(): Boolean {
        val items = listOf(
            MenuItem(OPTION_AGAIN, getString(R.string.scan_again)),
            MenuItem(OPTION_HELP, getString(R.string.scan_help)),
        )
        MenuSheet.show(requireContext(), items) { item ->
            when (item.id) {
                OPTION_AGAIN -> viewModel.onAction(ScanAction.ScanAgain)
                OPTION_HELP -> navigator.toSetupHelp()
            }
        }
        return true
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        ensureCameraPermissionThenStart()
    }

    override fun onPause() {
        resumed = false
        stopCamera()
        super.onPause()
    }

    private fun ensureCameraPermissionThenStart() {
        if (hasCameraPermission()) startCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
    }

    private fun hasCameraPermission() =
        ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED

    // ---- Surface ----

    override fun surfaceCreated(holder: SurfaceHolder) {
        surfaceReady = true
        startCamera()
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) = Unit

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        surfaceReady = false
        stopCamera()
    }

    // ---- Camera ----

    private fun startCamera() {
        val b = binding ?: return
        if (camera != null || !surfaceReady || !resumed || !hasCameraPermission()) return
        val cam = try {
            openBackCamera()
        } catch (e: RuntimeException) {
            null
        }
        if (cam == null) {
            viewModel.onAction(ScanAction.CameraFailed)
            return
        }
        try {
            val params = cam.parameters
            params.supportedPreviewSizes
                .minByOrNull { Math.abs(it.width * it.height - TARGET_PIXELS) }
                ?.let { params.setPreviewSize(it.width, it.height) }
            val modes = params.supportedFocusModes.orEmpty()
            when {
                Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE in modes ->
                    params.focusMode = Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE
                Camera.Parameters.FOCUS_MODE_AUTO in modes -> params.focusMode = Camera.Parameters.FOCUS_MODE_AUTO
            }
            cam.parameters = params
            val used = cam.parameters.previewSize
            previewWidth = used.width
            previewHeight = used.height
            val orientation = displayOrientation(cameraId)
            rotated = orientation == 90 || orientation == 270
            cam.setDisplayOrientation(orientation)
            cam.setPreviewDisplay(b.scanPreview.holder)
            val thread = HandlerThread("MatChatQr").also { it.start() }
            decodeThread = thread
            decodeHandler = Handler(thread.looper)
            cam.setPreviewCallback(this)
            cam.startPreview()
            camera = cam
            sizePreview()
            if (params.focusMode == Camera.Parameters.FOCUS_MODE_AUTO) autoFocus()
            viewModel.onAction(ScanAction.CameraStarted)
        } catch (e: java.io.IOException) {
            failStart(cam)
        } catch (e: RuntimeException) {
            failStart(cam)
        }
    }

    private fun failStart(cam: Camera) {
        cam.release()
        stopDecodeThread()
        viewModel.onAction(ScanAction.CameraFailed)
    }

    /** Fits the SurfaceView to the camera picture's ratio inside the area (no squash). */
    private fun sizePreview() {
        val b = binding ?: return
        if (camera == null) return
        val (w, h) = ScanLayout.fit(b.scanArea.width, b.scanArea.height, previewWidth, previewHeight, rotated)
        val params = b.scanPreview.layoutParams as FrameLayout.LayoutParams
        if (params.width != w || params.height != h) {
            params.width = w
            params.height = h
            b.scanPreview.layoutParams = params
        }
    }

    private fun openBackCamera(): Camera? {
        val count = Camera.getNumberOfCameras()
        if (count == 0) return null
        val info = Camera.CameraInfo()
        cameraId = (0 until count).firstOrNull {
            Camera.getCameraInfo(it, info)
            info.facing == Camera.CameraInfo.CAMERA_FACING_BACK
        } ?: 0
        return Camera.open(cameraId)
    }

    /** The standard rotation so the preview is upright on screen. */
    private fun displayOrientation(id: Int): Int {
        val info = Camera.CameraInfo()
        Camera.getCameraInfo(id, info)
        val degrees = when (requireActivity().windowManager.defaultDisplay.rotation) {
            Surface.ROTATION_90 -> 90
            Surface.ROTATION_180 -> 180
            Surface.ROTATION_270 -> 270
            else -> 0
        }
        return if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            (360 - (info.orientation + degrees) % 360) % 360
        } else {
            (info.orientation - degrees + 360) % 360
        }
    }

    private fun autoFocus() {
        try {
            camera?.autoFocus { _, _ -> }
        } catch (e: RuntimeException) {
            // Fixed-focus cameras and some OEM builds refuse; scanning still works.
        }
    }

    private fun stopCamera() {
        camera?.let {
            try {
                it.setPreviewCallback(null)
                it.stopPreview()
            } catch (e: RuntimeException) {
                // Already stopped.
            }
            it.release()
        }
        camera = null
        stopDecodeThread()
    }

    private fun stopDecodeThread() {
        decodeThread?.quitSafely()
        decodeThread = null
        decodeHandler = null
        busy = false
    }

    /** One frame at a time; frames arriving while one is decoding are dropped, never stored. */
    override fun onPreviewFrame(data: ByteArray?, cam: Camera?) {
        if (data == null || busy) return
        val handler = decodeHandler ?: return
        val w = previewWidth
        val h = previewHeight
        busy = true
        handler.post {
            val text = QrDecoder.decode(data, w, h)
            if (text == null) {
                busy = false
            } else {
                main.post {
                    if (binding != null) viewModel.onAction(ScanAction.Decoded(text))
                    busy = false
                }
            }
        }
    }

    override fun onDestroyView() {
        stopCamera()
        binding = null
        super.onDestroyView()
    }

    private companion object {
        const val OPTION_AGAIN = "again"
        const val OPTION_HELP = "help"

        /** About 640×480: enough for a QR code on a screen, light enough for a Snapdragon 215. */
        const val TARGET_PIXELS = 640 * 480
    }
}
