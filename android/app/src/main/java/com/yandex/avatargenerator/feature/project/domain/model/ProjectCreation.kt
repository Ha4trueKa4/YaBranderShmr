package com.yandex.avatargenerator.feature.project.domain.model

/**
 * Результат `POST /projects`: сам проект и id запущенной первичной
 * генерации. [jobId] равен `null`, если сервер его не прислал (старый бэкенд
 * или ветка копирования готового SVG) — тогда ждать нечего.
 */
data class ProjectCreation(val project: Project, val jobId: String?)
