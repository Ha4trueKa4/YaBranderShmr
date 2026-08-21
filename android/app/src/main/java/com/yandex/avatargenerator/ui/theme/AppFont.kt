package com.yandex.avatargenerator.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.yandex.avatargenerator.R

/**
 * Шрифт макета — Inter (лицензия SIL Open Font License, см. app/licenses/Inter-OFL-LICENSE.txt).
 * Файлы лежат в res/font; чтобы добавить недостающее начертание — положите .otf туда и
 * добавьте Font(...) сюда, весь остальной код ссылается только на [InterFontFamily].
 *
 * Здесь только те начертания, которые есть файлами и используются в UI. Регистрировать
 * начертание без файла нельзя: Compose не сможет его загрузить в рантайме.
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)
