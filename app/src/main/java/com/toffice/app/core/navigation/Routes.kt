package com.toffice.app.core.navigation

object Routes {
    const val DASHBOARD = "dashboard"

    // Office-style apps
    const val EDITOR = "editor"       // Word — مستندات
    const val SHEETS = "sheets"       // Excel — جداول بيانات
    const val NOTES = "notes"         // OneNote — ملاحظات
    const val CALENDAR = "calendar"   // Outlook — التقويم والمواعيد
    const val TASKS = "tasks"         // To Do — المهام
    const val EXPENSES = "expenses"   // الميزانية والمصروفات
    const val DOCUMENTS = "documents" // الملفات والمستندات

    const val LAUNCH = "launch"      // شاشة بدء: تفتح آخر مستند مباشرةً
    const val DOCS_LIST = "docs"     // قائمة المستندات (مستنداتي)

    const val EDITOR_DOC = "editor/{docId}"
    fun editorDoc(id: Long) = "editor/$id"

    // System
    const val BACKUP = "backup"
    const val SETTINGS = "settings"
}
