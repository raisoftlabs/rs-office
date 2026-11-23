package com.raisoftlabs.rsoffice.core.edit

/**
 * Original command-stack design inspired by the *idea* of grouped undo
 * transactions in large office engines. No LibreOffice code is copied.
 */
interface EditCommand {
    val label: String
    val mergeKey: String? get() = null

    fun execute()
    fun undo()

    fun mergeWith(next: EditCommand): Boolean = false
}

class UndoStack(private val maxEntries: Int = 100) {
    private val undo = ArrayDeque<EditCommand>()
    private val redo = ArrayDeque<EditCommand>()
    private val listeners = mutableListOf<() -> Unit>()
    private var transaction: TransactionCommand? = null

    var enabled: Boolean = true

    val canUndo: Boolean get() = undo.isNotEmpty()
    val canRedo: Boolean get() = redo.isNotEmpty()
    val undoLabel: String? get() = undo.lastOrNull()?.label
    val redoLabel: String? get() = redo.lastOrNull()?.label

    fun addListener(listener: () -> Unit) {
        listeners += listener
    }

    fun removeListener(listener: () -> Unit) {
        listeners -= listener
    }

    fun execute(command: EditCommand) {
        command.execute()
        val active = transaction
        if (active != null) {
            active.add(command)
        } else if (enabled) {
            val previous = undo.lastOrNull()
            if (previous == null || command.mergeKey == null || previous.mergeKey != command.mergeKey ||
                !previous.mergeWith(command)
            ) {
                undo.addLast(command)
                while (undo.size > maxEntries) undo.removeFirst()
            }
            redo.clear()
        }
        notifyListeners()
    }

    fun undo(): Boolean {
        val command = undo.removeLastOrNull() ?: return false
        command.undo()
        redo.addLast(command)
        notifyListeners()
        return true
    }

    fun redo(): Boolean {
        val command = redo.removeLastOrNull() ?: return false
        command.execute()
        undo.addLast(command)
        notifyListeners()
        return true
    }

    fun beginTransaction(label: String) {
        if (!enabled || transaction != null) return
        transaction = TransactionCommand(label)
    }

    fun endTransaction() {
        val finished = transaction ?: return
        transaction = null
        if (finished.isEmpty()) return
        undo.addLast(finished)
        while (undo.size > maxEntries) undo.removeFirst()
        redo.clear()
        notifyListeners()
    }

    fun clear() {
        undo.clear()
        redo.clear()
        transaction = null
        notifyListeners()
    }

    private fun notifyListeners() {
        listeners.toList().forEach { it() }
    }

    private class TransactionCommand(label: String) : EditCommand {
        override val label = label
        private val commands = mutableListOf<EditCommand>()

        fun add(command: EditCommand) {
            commands += command
        }

        fun isEmpty(): Boolean = commands.isEmpty()

        override fun execute() {
            commands.forEach { it.execute() }
        }

        override fun undo() {
            commands.asReversed().forEach { it.undo() }
        }
    }
}
