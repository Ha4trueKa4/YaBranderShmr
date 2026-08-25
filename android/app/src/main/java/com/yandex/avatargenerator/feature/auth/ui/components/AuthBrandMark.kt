package com.yandex.avatargenerator.feature.auth.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.yandex.avatargenerator.R

/**
 * Логотип-марка бренда (без текста) над заголовком формы входа/регистрации.
 * Общий для экранов входа и регистрации.
 */
@Composable
internal fun AuthBrandMark(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ic_logo_mark),
        contentDescription = null,
        modifier = modifier.size(48.dp),
    )
}