package com.defname.localshare.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldColors
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.VisualTransformation


@Composable
fun ConfirmedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    validator: (String) -> Boolean = { true },
    errorHint: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    var draftText by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }

    // Ein Hilfs-Flag, damit wir wissen, ob der Fokusverlust durch ein erfolgreiches "Go" kam
    var wasConfirmed by remember { mutableStateOf(false) }

    val isValid = remember(validator, draftText) {
        validator(draftText)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Synchronisation von außen (falls das ViewModel den Wert ändert)
    LaunchedEffect(value) {
        if (!isFocused) {
            draftText = value
        }
    }

    // Abbrechen via Hardware-Zurück-Taste / Geste
    if (isFocused) {
        BackHandler {
            draftText = value
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }


    TextField(
        value = draftText,
        onValueChange = {
            draftText = it
            wasConfirmed = false
        },
        label = label,
        modifier = modifier.onFocusChanged { focusState ->
            val previouslyFocused = isFocused
            isFocused = focusState.isFocused

            if (previouslyFocused && !isFocused) {
                if (!wasConfirmed) {
                    // Der User hat den Fokus verloren OHNE "Go" zu drücken -> Reset!
                    draftText = value
                }
                // Flag zurücksetzen für den nächsten Fokus-Zyklus
                wasConfirmed = false
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onAny = {
                if (isValid) {
                    wasConfirmed = true // 1. Als bestätigt markieren, BEVOR der Fokus verloren geht
                    onValueChange(draftText) // 2. Event ans ViewModel senden
                } else {
                    draftText = value
                }
                focusManager.clearFocus() // 3. Triggert onFocusChanged
                keyboardController?.hide()
            }
        ),
        isError = !isValid,
        supportingText = if (isValid) supportingText else errorHint,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        colors = colors,
    )
}



@Composable
fun OutlinedConfirmedTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    validator: (String) -> Boolean = { true },
    errorHint: @Composable (() -> Unit)? = null,
    enabled: Boolean = true,
    readOnly: Boolean = false,
    textStyle: TextStyle = LocalTextStyle.current,
    label: @Composable (() -> Unit)? = null,
    placeholder: @Composable (() -> Unit)? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    prefix: @Composable (() -> Unit)? = null,
    suffix: @Composable (() -> Unit)? = null,
    supportingText: @Composable (() -> Unit)? = null,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default.copy(imeAction = ImeAction.Done),
    singleLine: Boolean = false,
    maxLines: Int = if (singleLine) 1 else Int.MAX_VALUE,
    minLines: Int = 1,
    colors: TextFieldColors = OutlinedTextFieldDefaults.colors(),
) {
    var draftText by remember { mutableStateOf(value) }
    var isFocused by remember { mutableStateOf(false) }

    // Ein Hilfs-Flag, damit wir wissen, ob der Fokusverlust durch ein erfolgreiches "Go" kam
    var wasConfirmed by remember { mutableStateOf(false) }

    val isValid = remember(validator, draftText) {
        validator(draftText)
    }

    val focusManager = LocalFocusManager.current
    val keyboardController = LocalSoftwareKeyboardController.current

    // Synchronisation von außen (falls das ViewModel den Wert ändert)
    LaunchedEffect(value) {
        if (!isFocused) {
            draftText = value
        }
    }

    // Abbrechen via Hardware-Zurück-Taste / Geste
    if (isFocused) {
        BackHandler {
            draftText = value
            focusManager.clearFocus()
            keyboardController?.hide()
        }
    }


    OutlinedTextField(
        value = draftText,
        onValueChange = {
            draftText = it
            wasConfirmed = false
        },
        label = label,
        modifier = modifier.onFocusChanged { focusState ->
            val previouslyFocused = isFocused
            isFocused = focusState.isFocused

            if (previouslyFocused && !isFocused) {
                if (!wasConfirmed) {
                    // Der User hat den Fokus verloren OHNE "Go" zu drücken -> Reset!
                    draftText = value
                }
                // Flag zurücksetzen für den nächsten Fokus-Zyklus
                wasConfirmed = false
            }
        },
        keyboardOptions = keyboardOptions,
        keyboardActions = KeyboardActions(
            onAny = {
                if (isValid) {
                    wasConfirmed = true // 1. Als bestätigt markieren, BEVOR der Fokus verloren geht
                    onValueChange(draftText) // 2. Event ans ViewModel senden
                } else {
                    draftText = value
                }
                focusManager.clearFocus() // 3. Triggert onFocusChanged
                keyboardController?.hide()
            }
        ),
        isError = !isValid,
        supportingText = if (isValid) supportingText else errorHint,
        enabled = enabled,
        readOnly = readOnly,
        textStyle = textStyle,
        placeholder = placeholder,
        leadingIcon = leadingIcon,
        trailingIcon = trailingIcon,
        prefix = prefix,
        suffix = suffix,
        visualTransformation = visualTransformation,
        singleLine = singleLine,
        maxLines = maxLines,
        minLines = minLines,
        colors = colors,
    )
}