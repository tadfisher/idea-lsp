package com.tadfisher.idea.lsp.server

import com.intellij.openapi.editor.Caret
import com.intellij.openapi.editor.CaretAction
import com.intellij.openapi.editor.CaretModel
import com.intellij.openapi.editor.CaretState
import com.intellij.openapi.editor.Document
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.EditorGutter
import com.intellij.openapi.editor.EditorKind
import com.intellij.openapi.editor.EditorSettings
import com.intellij.openapi.editor.FoldingModel
import com.intellij.openapi.editor.IndentsModel
import com.intellij.openapi.editor.InlayModel
import com.intellij.openapi.editor.LogicalPosition
import com.intellij.openapi.editor.ScrollingModel
import com.intellij.openapi.editor.SelectionModel
import com.intellij.openapi.editor.SoftWrapModel
import com.intellij.openapi.editor.VisualPosition
import com.intellij.openapi.editor.colors.EditorColorsScheme
import com.intellij.openapi.editor.event.CaretListener
import com.intellij.openapi.editor.event.EditorMouseEventArea
import com.intellij.openapi.editor.event.EditorMouseListener
import com.intellij.openapi.editor.event.EditorMouseMotionListener
import com.intellij.openapi.editor.markup.MarkupModel
import com.intellij.openapi.editor.markup.TextAttributes
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Key
import java.awt.Insets
import java.awt.Point
import java.awt.event.MouseEvent
import java.awt.geom.Point2D
import javax.swing.JComponent
import javax.swing.border.Border

class FakeEditor @JvmOverloads constructor(
    private val myProject: Project,
    private val myDocument: Document,
    private val offset: Int = 0
) : Editor {

    private val caretModel by lazy { FakeCaretModel(offset) }

    override fun getProject(): Project? = myProject
    override fun getDocument(): Document = myDocument
    override fun getCaretModel(): CaretModel = caretModel

    override fun offsetToVisualPosition(offset: Int): VisualPosition { TODO("not implemented") }
    override fun offsetToVisualPosition(offset: Int, leanForward: Boolean, beforeSoftWrap: Boolean): VisualPosition { TODO("not implemented") }
    override fun getFoldingModel(): FoldingModel { TODO("not implemented") }
    override fun offsetToLogicalPosition(offset: Int): LogicalPosition { TODO("not implemented") }
    override fun getComponent(): JComponent { TODO("not implemented") }
    override fun <T : Any?> putUserData(key: Key<T>, value: T?) { TODO("not implemented") }
    override fun visualPositionToPoint2D(pos: VisualPosition): Point2D { TODO("not implemented") }
    override fun logicalPositionToOffset(pos: LogicalPosition): Int { TODO("not implemented") }
    override fun isViewer(): Boolean { TODO("not implemented") }
    override fun visualPositionToXY(visible: VisualPosition): Point { TODO("not implemented") }
    override fun getGutter(): EditorGutter { TODO("not implemented") }
    override fun logicalPositionToXY(pos: LogicalPosition): Point { TODO("not implemented") }
    override fun getScrollingModel(): ScrollingModel { TODO("not implemented") }
    override fun getIndentsModel(): IndentsModel { TODO("not implemented") }
    override fun getLineHeight(): Int { TODO("not implemented") }
    override fun getColorsScheme(): EditorColorsScheme { TODO("not implemented") }
    override fun isInsertMode(): Boolean { TODO("not implemented") }
    override fun getSelectionModel(): SelectionModel { TODO("not implemented") }
    override fun xyToLogicalPosition(p: Point): LogicalPosition { TODO("not implemented") }
    override fun getSoftWrapModel(): SoftWrapModel { TODO("not implemented") }
    override fun removeEditorMouseListener(listener: EditorMouseListener) { TODO("not implemented") }
    override fun isDisposed(): Boolean { TODO("not implemented") }
    override fun getEditorKind(): EditorKind { TODO("not implemented") }
    override fun addEditorMouseListener(listener: EditorMouseListener) { TODO("not implemented") }
    override fun getSettings(): EditorSettings { TODO("not implemented") }
    override fun xyToVisualPosition(p: Point): VisualPosition { TODO("not implemented") }
    override fun xyToVisualPosition(p: Point2D): VisualPosition { TODO("not implemented") }
    override fun isColumnMode(): Boolean { TODO("not implemented") }
    override fun getMouseEventArea(e: MouseEvent): EditorMouseEventArea? { TODO("not implemented") }
    override fun setBorder(border: Border?) { TODO("not implemented") }
    override fun getMarkupModel(): MarkupModel { TODO("not implemented") }
    override fun visualToLogicalPosition(visiblePos: VisualPosition): LogicalPosition { TODO("not implemented") }
    override fun getInsets(): Insets { TODO("not implemented") }
    override fun addEditorMouseMotionListener(listener: EditorMouseMotionListener) { TODO("not implemented") }
    override fun logicalToVisualPosition(logicalPos: LogicalPosition): VisualPosition { TODO("not implemented") }
    override fun isOneLineMode(): Boolean { TODO("not implemented") }
    override fun getInlayModel(): InlayModel { TODO("not implemented") }
    override fun setHeaderComponent(header: JComponent?) { TODO("not implemented") }
    override fun <T : Any?> getUserData(key: Key<T>): T? { TODO("not implemented") }
    override fun getHeaderComponent(): JComponent? { TODO("not implemented") }
    override fun removeEditorMouseMotionListener(listener: EditorMouseMotionListener) { TODO("not implemented") }
    override fun hasHeaderComponent(): Boolean { TODO("not implemented") }
    override fun getContentComponent(): JComponent { TODO("not implemented") }
}

class FakeCaretModel @JvmOverloads constructor(
    private val myOffset: Int = 0
) : CaretModel {

    override fun getOffset(): Int = myOffset

    override fun getVisualLineStart(): Int { TODO("not implemented") }
    override fun removeSecondaryCarets() { TODO("not implemented") }
    override fun removeCaret(caret: Caret): Boolean { TODO("not implemented") }
    override fun getPrimaryCaret(): Caret { TODO("not implemented") }
    override fun getTextAttributes(): TextAttributes { TODO("not implemented") }
    override fun removeCaretListener(listener: CaretListener) { TODO("not implemented") }
    override fun getCaretAt(pos: VisualPosition): Caret? { TODO("not implemented") }
    override fun isUpToDate(): Boolean { TODO("not implemented") }
    override fun getCaretCount(): Int { TODO("not implemented") }
    override fun moveCaretRelatively(columnShift: Int, lineShift: Int, withSelection: Boolean, blockSelection: Boolean, scrollToCaret: Boolean) { TODO("not implemented") }
    override fun setCaretsAndSelections(caretStates: MutableList<CaretState>) { TODO("not implemented") }
    override fun setCaretsAndSelections(caretStates: MutableList<CaretState>, updateSystemSelection: Boolean) { TODO("not implemented") }
    override fun getLogicalPosition(): LogicalPosition { TODO("not implemented") }
    override fun addCaretListener(listener: CaretListener) { TODO("not implemented") }
    override fun moveToVisualPosition(pos: VisualPosition) { TODO("not implemented") }
    override fun supportsMultipleCarets(): Boolean { TODO("not implemented") }
    override fun getCaretsAndSelections(): MutableList<CaretState> { TODO("not implemented") }
    override fun getVisualLineEnd(): Int { TODO("not implemented") }
    override fun addCaret(pos: VisualPosition): Caret? { TODO("not implemented") }
    override fun addCaret(pos: VisualPosition, makePrimary: Boolean): Caret? { TODO("not implemented") }
    override fun runForEachCaret(action: CaretAction) { TODO("not implemented") }
    override fun runForEachCaret(action: CaretAction, reverseOrder: Boolean) { TODO("not implemented") }
    override fun getAllCarets(): MutableList<Caret> { TODO("not implemented") }
    override fun runBatchCaretOperation(runnable: Runnable) { TODO("not implemented") }
    override fun moveToLogicalPosition(pos: LogicalPosition) { TODO("not implemented") }
    override fun moveToOffset(offset: Int) { TODO("not implemented") }
    override fun moveToOffset(offset: Int, locateBeforeSoftWrap: Boolean) { TODO("not implemented") }
    override fun getCurrentCaret(): Caret { TODO("not implemented") }
    override fun getVisualPosition(): VisualPosition { TODO("not implemented") }
}
