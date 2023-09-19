import androidx.compose.ui.Modifier
import androidx.compose.ui.awt.ComposeWindow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.toAwtImage
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import io.ktor.utils.io.core.*
import io.ktor.utils.io.streams.*
import java.awt.Cursor
import java.awt.Point
import java.awt.datatransfer.DataFlavor
import java.awt.datatransfer.Transferable
import java.awt.dnd.*
import java.io.File
import java.io.Serializable
import java.nio.file.Files
import java.awt.dnd.DragSource as AwtDragSource
import java.awt.dnd.DropTarget as AwtDropTarget

fun Modifier.rootDragDropModifier(
    density: Float,
    window: ComposeWindow,
): Modifier = this then RootDragDropElement(
    density = density,
    window = window,
)

private data class RootDragDropElement(
    val density: Float,
    val window: ComposeWindow,
) : ModifierNodeElement<RootDragDropNode>() {

    override fun create() = RootDragDropNode(
        density = density,
        window = window,
    )

    override fun update(node: RootDragDropNode) = with(node) {
        density = this@RootDragDropElement.density
        window = this@RootDragDropElement.window
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "RootDragDropNode"
    }

    override fun hashCode(): Int = 0

    override fun equals(other: Any?): Boolean = this === other
}

internal class RootDragDropNode(
    var density: Float,
    var window: ComposeWindow,
) : DelegatingNode(),
    ModifierLocalModifierNode,
    GlobalPositionAwareModifierNode {

    private val dragDropNode: DragDropNode = delegate(rootDragDropNode())

    private val dropTargetListener = DensityAwareDropTargetListener(
        dragDropNode = dragDropNode,
        density = density
    )

    private val dragGestureListener = DensityAwareDragGestureListener(
        dragDroppable = dragDropNode,
        density = density
    )

    init {
        window.contentPane.dropTarget = AwtDropTarget().apply {
            addDropTargetListener(dropTargetListener)
        }
        AwtDragSource.getDefaultDragSource().createDefaultDragGestureRecognizer(
            window,
            DnDConstants.ACTION_COPY,
            dragGestureListener
        )
    }

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)

    fun updateDensity(density: Float) {
        dropTargetListener.density = density
        dragGestureListener.density = density
    }
}

private class DensityAwareDropTargetListener(
    private val dragDropNode: DragDroppable,
    var density: Float,
) : DropTargetListener {
    override fun dragEnter(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dragDropNode.onStarted(
            dtde.startDragMimeTypes(),
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
        dragDropNode.onEntered()
    }

    override fun dragOver(dtde: DropTargetDragEvent?) {
        if (dtde == null) return
        dragDropNode.onMoved(
            Offset(
                dtde.location.x * density,
                dtde.location.y * density
            )
        )
    }

    override fun dropActionChanged(dtde: DropTargetDragEvent?) = Unit

    override fun dragExit(dte: DropTargetEvent?) {
        dragDropNode.onExited()
        dragDropNode.onEnded()
    }

    override fun drop(dtde: DropTargetDropEvent?) {
        if (dtde == null) return dragDropNode.onEnded()

        dtde.acceptDrop(DnDConstants.ACTION_REFERENCE)
        dtde.dropComplete(
            dragDropNode.onDropped(
                dtde.endDropUris(),
                Offset(
                    dtde.location.x * density,
                    dtde.location.y * density
                )
            )
        )
        dragDropNode.onEnded()
    }
}

private class DensityAwareDragGestureListener(
    private val dragDroppable: DragDroppable,
    var density: Float,
) : DragGestureListener {
    override fun dragGestureRecognized(event: DragGestureEvent) {
        val offset = Offset(
            x = event.dragOrigin.x * density,
            y = event.dragOrigin.y * density
        )

        val dragInfo = dragDroppable.dragInfo(offset) ?: return

        if (dragInfo.uris.isNotEmpty()) when (val shadowPainter = dragInfo.dragShadowPainter) {
            null -> event.startDrag(
                /* dragCursor = */ Cursor.getDefaultCursor(),
                /* transferable = */ dragInfo.transferable()
            )

            else -> event.startDrag(
                /* dragCursor = */ Cursor.getDefaultCursor(),
                /* dragImage = */
                shadowPainter.toAwtImage(
                    density = Density(density),
                    layoutDirection = LayoutDirection.Ltr,
                    size = dragInfo.size
                ),
                /* imageOffset = */
                Point(-dragInfo.size.width.toInt() / 2, -dragInfo.size.height.toInt() / 2),
                /* transferable = */
                dragInfo.transferable(),
                /* dsl = */
                object : DragSourceAdapter() {
                }
            )
        }
    }
}

private fun DropTargetDragEvent.startDragMimeTypes(): Set<String> =
    transferable
        .transferDataFlavors
        .filter(transferable::isDataFlavorSupported)
        .map(::uris)
        .flatten()
        .map(Uri::mimetype)
        .toSet()

private fun DropTargetDropEvent.endDropUris(): List<Uri> =
    transferable
        .transferDataFlavors
        .filter(transferable::isDataFlavorSupported)
        .map(::uris)
        .flatten()

private fun DropTargetEvent.uris(dataFlavor: DataFlavor?): List<Uri> {
    val transferable = when (this) {
        is DropTargetDragEvent -> transferable
        is DropTargetDropEvent -> transferable
        else -> null
    } ?: return emptyList()

    return when (dataFlavor) {
        DataFlavor.javaFileListFlavor ->
            transferable.getTransferData(dataFlavor)
                .let { it as? List<*> ?: listOf<File>() }
                .filterIsInstance<File>()
                .map(::FileUri)

        ArrayListFlavor ->
            transferable.getTransferData(dataFlavor)
                .let { it as? List<*> ?: listOf<SerializableUri>() }
                .filterIsInstance<SerializableUri>()

        else -> listOf()
    }
}

val ArrayListFlavor = DataFlavor(
    java.util.ArrayList::class.java,
    "ArrayList"
)

private fun DragInfo.transferable() = object : Transferable {
    override fun getTransferDataFlavors(): Array<DataFlavor> =
        arrayOf(ArrayListFlavor)

    override fun isDataFlavorSupported(flavor: DataFlavor?): Boolean =
        flavor == ArrayListFlavor

    override fun getTransferData(flavor: DataFlavor?): Any =
        uris.toSerializableList()
}

private fun List<Uri>.toSerializableList() = java.util.ArrayList(
    map { SerializableUri(path = it.path, mimetype = it.mimetype) }
)

private class SerializableUri(
    override val path: String,
    override val mimetype: String,
) : Uri, Serializable

interface Uri {
    val path: String
    val mimetype: String
}

interface LocalUri : Uri

data class RemoteUri(
    override val path: String,
    override val mimetype: String,
) : Uri

interface UriConverter {
    fun toInput(uri: LocalUri): Input
    suspend fun name(uri: LocalUri): String

    suspend fun mimeType(uri: LocalUri): String

    suspend fun contentLength(uri: LocalUri): Long?
}

data class FileDesc(
    val name: String,
    val mimetype: String,
    val binaryData: Input,
    val contentSize: Long? = null,
)

suspend fun UriConverter.fileDesc(uri: LocalUri) = FileDesc(
    name = name(uri),
    mimetype = mimeType(uri),
    binaryData = toInput(uri),
    contentSize = contentLength(uri),
)

class ActualUriConverter : UriConverter {
    override fun toInput(uri: LocalUri): Input = when (uri) {
        is FileUri -> File(uri.path).inputStream().asInput()
        else -> throw IllegalArgumentException("Unknown URI type")
    }

    override suspend fun name(uri: LocalUri): String = when (uri) {
        is FileUri -> File(uri.path).name
        else -> throw IllegalArgumentException("Unknown URI type")
    }

    override suspend fun mimeType(uri: LocalUri): String = when (uri) {
        is FileUri -> Files.probeContentType(uri.file.toPath())
        else -> throw IllegalArgumentException("Unknown URI type")
    }

    override suspend fun contentLength(uri: LocalUri): Long? = when (uri) {
        is FileUri -> uri.file.length()
        else -> throw IllegalArgumentException("Unknown URI type")
    }
}

data class FileUri(
    val file: File,
) : LocalUri {
    override val path: String
        get() = file.path

    override val mimetype: String
        get() = Files.probeContentType(file.toPath())
}