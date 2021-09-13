package androidx.camera.core

internal class ImageProxyWrapper(image: ImageProxy) : ForwardingImageProxy(image) {
    companion object {
        fun wrap(image: ImageProxy): ImageProxy {
            if (image is ForwardingImageProxy) {
                return image
            } else {
                return ImageProxyWrapper(image)
            }
        }

        fun addOnCloseListener(image: ImageProxy, listener: () -> Unit) {
            (image as ForwardingImageProxy).addOnImageCloseListener { listener() }
        }
    }
}