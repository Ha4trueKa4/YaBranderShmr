package com.yandex.avatargenerator.ui.theme

import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import com.yandex.avatargenerator.R

/**
 * Шрифт макета — Inter (лицензия SIL Open Font License, см. app/licenses/Inter-OFL-LICENSE.txt).
 * Файлы лежат в res/font; чтобы добавить недостающее начертание — положите .otf туда и
 * добавьте Font(...) сюда, весь остальной код ссылается только на [InterFontFamily].
 */
val InterFontFamily = FontFamily(
    Font(R.font.inter_thin, FontWeight.Thin),
    Font(R.font.inter_extralight, FontWeight.ExtraLight),
    Font(R.font.inter_light, FontWeight.Light),
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold),
    Font(R.font.inter_extrabold, FontWeight.ExtraBold),
    Font(R.font.inter_black, FontWeight.Black),
    Font(R.font.inter_italic, FontWeight.Normal, FontStyle.Italic)
)
