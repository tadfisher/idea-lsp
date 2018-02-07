package com.tadfisher.idea.lsp

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.Ref
import com.intellij.psi.PsiElement
import com.intellij.refactoring.RefactoringImpl
import com.intellij.refactoring.RenameRefactoring
import com.intellij.refactoring.listeners.RefactoringEventData
import com.intellij.refactoring.listeners.RefactoringEventListener
import com.intellij.refactoring.rename.RenameProcessor
import com.intellij.refactoring.rename.RenamePsiElementProcessor
import com.intellij.refactoring.rename.RenameUtil
import com.intellij.refactoring.rename.naming.AutomaticRenamer
import com.intellij.usageView.UsageInfo
import com.intellij.util.containers.MultiMap

class LspRenameProcessor(
    project: Project,
    private val primaryElement: PsiElement,
    private val newName: String,
    isSearchInComments: Boolean,
    isSearchTextOccurrences: Boolean
) : RenameProcessor(project, primaryElement, newName, isSearchInComments, isSearchTextOccurrences) {

    init { myPrepareSuccessfulSwingThreadCallback = null }

    override fun preprocessUsages(refUsages: Ref<Array<UsageInfo>>): Boolean {
        val usagesIn = refUsages.get()
        val conflicts = MultiMap<PsiElement, String>()

        RenameUtil.addConflictDescriptions(usagesIn, conflicts)
        RenamePsiElementProcessor.forElement(primaryElement)
            .findExistingNameConflicts(primaryElement, newName, conflicts, myAllRenames)
        if (!conflicts.isEmpty) {
            // TODO log this
            val conflictData = RefactoringEventData()
            conflictData.putUserData(RefactoringEventData.CONFLICTS_KEY, conflicts.values())
            myProject.messageBus.syncPublisher(RefactoringEventListener.REFACTORING_EVENT_TOPIC)
                .conflictsDetected("refactoring.rename", conflictData)
            return false
        }

        return super.preprocessUsages(refUsages)
    }

    override fun showAutomaticRenamingDialog(automaticVariableRenamer: AutomaticRenamer): Boolean {
        for (element in automaticVariableRenamer.elements) {
            automaticVariableRenamer.setRename(element, automaticVariableRenamer.getNewName(element))
        }
        return true
    }

    override fun isPreviewUsages(): Boolean = false
    override fun isPreviewUsages(usages: Array<UsageInfo>): Boolean = false
    override fun setPreviewUsages(isPreviewUsages: Boolean) {}
    override fun setPrepareSuccessfulSwingThreadCallback(prepareSuccessfulSwingThreadCallback: Runnable?) {}
}

class LspRenameRefactoring(
    project: Project,
    element: PsiElement,
    newName: String,
    isSearchInComments: Boolean,
    isSearchTextOccurrences: Boolean
) : RefactoringImpl<LspRenameProcessor>(LspRenameProcessor(project, element, newName, isSearchInComments, isSearchTextOccurrences)), RenameRefactoring {

    override fun addElement(element: PsiElement, newName: String) = myProcessor.addElement(element, newName)
    override fun getElements(): Set<PsiElement> = myProcessor.elements
    override fun getNewNames(): Collection<String> = myProcessor.newNames
    override fun setSearchInComments(value: Boolean) { myProcessor.isSearchInComments = value }
    override fun setSearchInNonJavaFiles(value: Boolean) { myProcessor.isSearchTextOccurrences = value }
    override fun isSearchInComments(): Boolean = myProcessor.isSearchInComments
    override fun isSearchInNonJavaFiles(): Boolean = myProcessor.isSearchTextOccurrences
    override fun isInteractive(): Boolean = false
    override fun setInteractive(prepareSuccessfulCallback: Runnable?) = super.setInteractive(null)
    override fun shouldPreviewUsages(usages: Array<UsageInfo>): Boolean = false
    override fun setPreviewUsages(value: Boolean) {}
}
