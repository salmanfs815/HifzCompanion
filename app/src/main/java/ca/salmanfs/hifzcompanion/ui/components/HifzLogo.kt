package ca.salmanfs.hifzcompanion.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import ca.salmanfs.hifzcompanion.R

@Composable
fun HifzLogo(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.hifz_logo),
        contentDescription = "Hifz Companion Logo",
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
