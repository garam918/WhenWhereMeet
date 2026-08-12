package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmInfoPanel
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.component.WwmTopBar

@Composable
fun JoinRoomScreen(
    initialRoomCode: String,
    onBack: () -> Unit,
    onJoin: (String, String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var roomCode by remember(initialRoomCode) { mutableStateOf(initialRoomCode.filter(Char::isLetterOrDigit).uppercase().take(6)) }
    var nickname by remember { mutableStateOf("") }
    val keyboardController = LocalSoftwareKeyboardController.current
    val doneKeyboardActions = KeyboardActions(onDone = { keyboardController?.hide() })
    val canJoin = roomCode.length == 6 && nickname.isNotBlank()

    Column(modifier.fillMaxSize().background(WwmBackground)) {
        WwmTopBar(title = "언제어디", leadingText = "‹", onLeadingClick = onBack)
        Column(
            Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Box(Modifier.size(52.dp).background(WwmSoftIndigo, RoundedCornerShape(16.dp)), contentAlignment = Alignment.Center) {
                Text("#", color = WwmIndigo, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.height(20.dp))
            Text("방 참여하기", color = WwmText, fontSize = 26.sp, fontWeight = FontWeight.Bold)
            Spacer(Modifier.height(8.dp))
            Text(
                "초대받은 코드와 사용할 닉네임을 입력해주세요.",
                color = WwmMuted,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(30.dp))
            Text("방 코드", color = WwmText, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.fillMaxWidth())
            Spacer(Modifier.height(10.dp))
            RoomCodeField(
                value = roomCode,
                onValueChange = { roomCode = it.filter(Char::isLetterOrDigit).uppercase().take(6) },
            )
            Spacer(Modifier.height(22.dp))
            OutlinedTextField(
                value = nickname,
                onValueChange = { nickname = it },
                label = { Text("닉네임") },
                placeholder = { Text("어떤 이름으로 참여할까요?") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = doneKeyboardActions,
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(16.dp))
            WwmInfoPanel(
                title = "로그인 없이 바로 참여할 수 있어요",
                description = "닉네임은 이 약속방에서만 다른 참여자에게 보여요.",
                icon = "i",
            )
            Spacer(Modifier.weight(1f))
            WwmPrimaryButton(
                text = "약속방 참여하기",
                onClick = { onJoin(roomCode, nickname.trim()) },
                enabled = canJoin,
            )
        }
    }
}

@Composable
private fun RoomCodeField(value: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        singleLine = true,
        textStyle = TextStyle(color = Color.Transparent),
        cursorBrush = SolidColor(Color.Transparent),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Ascii, imeAction = ImeAction.Next),
        modifier = Modifier.fillMaxWidth().semantics { contentDescription = "방 코드 6자리" },
        decorationBox = { innerTextField ->
            Box {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    repeat(6) { index ->
                        val character = value.getOrNull(index)?.toString().orEmpty()
                        val active = index == value.length.coerceAtMost(5)
                        Box(
                            Modifier.size(width = 48.dp, height = 54.dp)
                                .background(Color.White, RoundedCornerShape(12.dp))
                                .border(1.5.dp, if (active) WwmIndigo else WwmBorder, RoundedCornerShape(12.dp)),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(character, color = WwmText, fontSize = 19.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
                Box(Modifier.size(1.dp)) { innerTextField() }
            }
        },
    )
}
