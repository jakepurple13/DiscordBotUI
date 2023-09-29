import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.LayoutCoordinates
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.modifier.ModifierLocalMap
import androidx.compose.ui.modifier.ModifierLocalModifierNode
import androidx.compose.ui.modifier.modifierLocalMapOf
import androidx.compose.ui.modifier.modifierLocalOf
import androidx.compose.ui.node.DelegatingNode
import androidx.compose.ui.node.GlobalPositionAwareModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.platform.InspectorInfo
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.toSize

@Composable
fun LazyListState.isScrollingUp(): Boolean {
    var previousIndex by remember(this) { mutableIntStateOf(firstVisibleItemIndex) }
    var previousScrollOffset by remember(this) { mutableIntStateOf(firstVisibleItemScrollOffset) }
    return remember(this) {
        derivedStateOf {
            if (previousIndex != firstVisibleItemIndex) {
                previousIndex > firstVisibleItemIndex
            } else {
                previousScrollOffset >= firstVisibleItemScrollOffset
            }.also {
                previousIndex = firstVisibleItemIndex
                previousScrollOffset = firstVisibleItemScrollOffset
            }
        }
    }.value
}

private val ModifierLocalDragDropParent = modifierLocalOf<DragDropParent?> { null }

internal class DragDropNode(
    private val onDragOrDropStarted: (DragOrDropStart) -> DragOrDrop?,
) : Modifier.Node(),
    GlobalPositionAwareModifierNode,
    ModifierLocalModifierNode,
    DragDropParent,
    DragDropChild,
    DragDroppable {

    private val parent: DragDropParent?
        get() = ModifierLocalDragDropParent.current

    override val children = mutableListOf<DragDropChild>()
    override var coordinates: LayoutCoordinates? = null
    private var activeChild: DragDropChild? = null
    private var currentTarget: DropTarget? = null

    override val providedValues: ModifierLocalMap = modifierLocalMapOf(ModifierLocalDragDropParent to this)

    // start Node
    override fun onAttach() {
        parent?.registerChild(this)
        super.onAttach()
    }

    override fun onDetach() {
        parent?.unregisterChild(this)
        currentTarget = null
        super.onDetach()
    }
    // end Node

    // start DropTargetParent
    override fun registerChild(child: DragDropChild) {
        children += child
        //if a drag is in progress, check if we need to send events
    }

    override fun unregisterChild(child: DragDropChild) {
        children -= child
    }
    // end DropTargetParent

    // start DropTargetNode
    override val size: IntSize
        get() = when (val coordinates = coordinates) {
            null -> IntSize.Zero
            else -> coordinates.size
        }
    // end DropTargetNode


    // start DragSource
    override val dragShadowPainter: Painter?
        get() = null

    override fun dragInfo(offset: Offset): DragInfo? {
        coordinates ?: return null

        var smallestDraggedChild: DragDropChild? = smallestChildWithin(offset)
            ?.takeUnless(this::equals)

        // Attempt to drag the smallest child within the bounds first
        val childDragStatus = smallestDraggedChild?.dragInfo(offset)
        if (childDragStatus != null) return childDragStatus

        // No draggable child, attempt to drag self
        return when (val dragSource = onDragOrDropStarted(DragOrDropStart.Drag(offset))) {
            is DragSource -> dragSource.dragInfo(offset)
            is DropTarget -> throw IllegalArgumentException("Attempted to start drag in a drop target")
            null -> null
        }
    }
    // end DragSource

    // start DropTarget
    override fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean {
        coordinates ?: return false

        check(currentTarget == null)
        currentTarget = when (val dropTarget = onDragOrDropStarted(DragOrDropStart.Drop(mimeTypes, position))) {
            is DragSource -> throw IllegalArgumentException("Attempted to start drop in a drag source")
            is DropTarget -> dropTarget
            null -> null
        }

        var handledByChild = false

        children.forEach { child ->
            handledByChild = handledByChild or child.onStarted(
                mimeTypes = mimeTypes,
                position = position
            )
        }
        return handledByChild || currentTarget != null
    }

    override fun onEntered() {
        currentTarget?.onEntered()
    }

    override fun onMoved(position: Offset) {
        coordinates ?: return
        val currentActiveChild: DragDropChild? = activeChild

        val newChild: DragDropChild? = when (currentActiveChild != null && currentActiveChild.contains(position)) {
            // Moved within child.
            true -> currentActiveChild
            // Position is now outside active child, maybe it entered a different one.
            false -> children.firstOrNull { it.contains(position) }
        }
        when {
            // Left us and went to a child.
            newChild != null && currentActiveChild == null -> {
                currentTarget?.onExited()
                newChild.dispatchEntered(position)
            }
            // Left the child and returned to us.
            newChild == null && currentActiveChild != null -> {
                currentActiveChild.onExited()
                currentTarget?.dispatchEntered(position)
            }
            // Left one child and entered another.
            newChild != currentActiveChild -> {
                currentActiveChild?.onExited()
                newChild?.dispatchEntered(position)
            }
            // Stayed in the same child.
            newChild != null -> newChild.onMoved(position)
            // Stayed in us.
            else -> currentTarget?.onMoved(position)
        }

        this.activeChild = newChild
    }

    override fun onExited() {
//        activeChild?.onDragExited()
//        activeChild = null
        currentTarget?.onExited()
    }

    override fun onDropped(uris: List<Uri>, position: Offset): Boolean =
        when (val currentActiveChild = activeChild) {
            null -> currentTarget?.onDropped(
                uris = uris,
                position = position
            ) ?: false

            else -> currentActiveChild.onDropped(
                uris = uris,
                position = position
            )
        }

    override fun onEnded() {
        children.forEach { it.onEnded() }
        currentTarget?.onEnded()
        currentTarget = null
    }
    // end DropTarget

    // start OnGloballyPositionedModifier
    override fun onGloballyPositioned(coordinates: LayoutCoordinates) {
        this.coordinates = coordinates
    }
    // end OnGloballyPositionedModifier

}

private fun DropTarget.dispatchEntered(position: Offset) {
    onEntered()
    onMoved(position)
}

private fun DragDropChild.contains(position: Offset): Boolean {
    val currentCoordinates = coordinates ?: return false
    if (!currentCoordinates.isAttached) return false

    val (width, height) = currentCoordinates.size
    val (x1, y1) = currentCoordinates.positionInRoot()
    val x2 = x1 + width
    val y2 = y1 + height

    return position.x in x1..x2 && position.y in y1..y2
}

private fun DragDropChild.smallestChildWithin(offset: Offset): DragDropChild? {
    if (children.isEmpty() && contains(offset)) return this

    var smallestChild: DragDropChild? = null
    children.forEach { child ->
        val smallestInnerChild = child.smallestChildWithin(offset)
        if (
            smallestInnerChild != null &&
            smallestInnerChild.area < (smallestChild?.area ?: Int.MAX_VALUE)
        ) smallestChild = child
    }

    return smallestChild
}

internal sealed interface DragOrDrop

internal interface DragDroppable : DragSource, DropTarget

/**
 * Root level [Modifier.Node], it always rejects leaving acceptance to its children
 */
internal fun rootDragDropNode() = DragDropNode(
    onDragOrDropStarted = { _ -> null }
)

internal interface DragDropParent {

    val children: List<DragDropChild>

    fun registerChild(child: DragDropChild)
    fun unregisterChild(child: DragDropChild)
}

internal sealed interface DragDropChild : DragDroppable, DragDropParent {
    val coordinates: LayoutCoordinates?
}

internal val DragDropChild.area
    get() = size.width * size.height

internal sealed class DragOrDropStart {
    data class Drag(
        val offset: Offset,
    ) : DragOrDropStart()

    data class Drop(
        val mimeTypes: Set<String>,
        val offset: Offset,
    ) : DragOrDropStart()
}

internal sealed interface DropTarget : DragOrDrop {
    fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean
    fun onEntered()
    fun onMoved(position: Offset) {}
    fun onExited()
    fun onDropped(uris: List<Uri>, position: Offset): Boolean
    fun onEnded()
}

fun Modifier.dropTarget(
    onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    onEntered: () -> Unit = { },
    onMoved: (position: Offset) -> Unit = {},
    onExited: () -> Unit = { },
    onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    onEnded: () -> Unit = {},
): Modifier = this then DropTargetElement(
    onStarted = onStarted,
    onEntered = onEntered,
    onMoved = onMoved,
    onExited = onExited,
    onDropped = onDropped,
    onEnded = onEnded
)

private data class DropTargetElement(
    val onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    val onEntered: () -> Unit = { },
    val onMoved: (position: Offset) -> Unit = {},
    val onExited: () -> Unit = { },
    val onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    val onEnded: () -> Unit = {},
) : ModifierNodeElement<DropTargetNode>() {
    override fun create() = DropTargetNode(
        onStarted = onStarted,
        onEntered = onEntered,
        onMoved = onMoved,
        onExited = onExited,
        onDropped = onDropped,
        onEnded = onEnded,
    )

    override fun update(node: DropTargetNode) = with(node) {
        onStarted = this@DropTargetElement.onStarted
        onEntered = this@DropTargetElement.onEntered
        onMoved = this@DropTargetElement.onMoved
        onExited = this@DropTargetElement.onExited
        onDropped = this@DropTargetElement.onDropped
        onEnded = this@DropTargetElement.onEnded
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dropTarget"
        properties["onDragStarted"] = onStarted
        properties["onEntered"] = onEntered
        properties["onMoved"] = onMoved
        properties["onExited"] = onExited
        properties["onDropped"] = onDropped
        properties["onEnded"] = onEnded
    }
}

internal sealed interface DragSource : DragOrDrop {

    val size: IntSize

    val dragShadowPainter: Painter?

    fun dragInfo(offset: Offset): DragInfo?
}

internal data class DragInfo(
    val size: Size,
    val uris: List<Uri>,
    val dragShadowPainter: Painter?,
)

fun Modifier.dragSource(
    dragShadowPainter: Painter? = null,
    uris: List<Uri>,
): Modifier = this then DragSourceElement(
    dragShadowPainter = dragShadowPainter,
    uris = uris
)

private data class DragSourceElement(
    /**
     * Optional painter to draw the drag shadow for the UI.
     * If not provided, the system default will be used.
     */
    val dragShadowPainter: Painter? = null,
    /**
     * A items to be transferred. If empty, a drag gesture will not be started.
     */
    val uris: List<Uri>,
) : ModifierNodeElement<DragSourceNode>() {
    override fun create() = DragSourceNode(
        dragShadowPainter = dragShadowPainter,
        uris = uris
    )

    override fun update(node: DragSourceNode) = with(node) {
        dragShadowPainter = this@DragSourceElement.dragShadowPainter
        uris = this@DragSourceElement.uris
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "dragSource"
        properties["dragShadowPainter"] = dragShadowPainter
        properties["dragStatus"] = uris
    }
}

private class DragSourceNode(
    override var dragShadowPainter: Painter?,
    var uris: List<Uri>,
) : DelegatingNode(),
    ModifierLocalModifierNode,
    GlobalPositionAwareModifierNode,
    DragSource {

    private val dragDropNode = delegate(
        DragDropNode { start ->
            when (start) {
                is DragOrDropStart.Drop -> null
                is DragOrDropStart.Drag -> when (uris) {
                    emptyList<Uri>() -> null
                    else -> this@DragSourceNode
                }
            }
        }
    )

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override val size: IntSize get() = dragDropNode.size

    override fun dragInfo(offset: Offset): DragInfo? =
        when (uris) {
            emptyList<Uri>() -> null
            else -> DragInfo(
                uris = uris,
                size = size.toSize(),
                dragShadowPainter = dragShadowPainter
            )
        }

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)
}

private class DropTargetNode(
    var onStarted: (mimeTypes: Set<String>, Offset) -> Boolean,
    var onEntered: () -> Unit,
    var onMoved: (position: Offset) -> Unit,
    var onExited: () -> Unit,
    var onDropped: (uris: List<Uri>, position: Offset) -> Boolean,
    var onEnded: () -> Unit,
) : DelegatingNode(),
    ModifierLocalModifierNode,
    GlobalPositionAwareModifierNode,
    DropTarget {

    private val dragDropNode = delegate(
        DragDropNode { start ->
            when (start) {
                is DragOrDropStart.Drag -> null
                is DragOrDropStart.Drop -> when {
                    onStarted(start.mimeTypes, start.offset) -> this@DropTargetNode
                    else -> null
                }
            }
        }
    )

    override val providedValues: ModifierLocalMap = dragDropNode.providedValues

    override fun onStarted(mimeTypes: Set<String>, position: Offset): Boolean = onStarted.invoke(
        mimeTypes,
        dragDropNode.coordinates?.windowToLocal(position) ?: position
    )

    override fun onEntered() = onEntered.invoke()

    override fun onMoved(position: Offset) = onMoved.invoke(
        dragDropNode.coordinates?.windowToLocal(position) ?: position
    )

    override fun onExited() = onExited.invoke()

    override fun onDropped(uris: List<Uri>, position: Offset): Boolean = onDropped.invoke(
        uris,
        dragDropNode.coordinates?.windowToLocal(position) ?: position
    )

    override fun onEnded() = onEnded.invoke()

    override fun onGloballyPositioned(coordinates: LayoutCoordinates) =
        dragDropNode.onGloballyPositioned(coordinates)
}

inline fun <T, R> Iterable<T>.foldWithFirst(initial: (T) -> R, operation: (acc: R, T) -> R): R {
    val first = first()
    val f = initial(first)
    return drop(1).fold(initial = f, operation = operation)
}