package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmIndigo
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
    var roomCode by remember(initialRoomCode) { mutableStateOf(initialRoomCode) }
    var nickname by remember { mutableStateOf("") }

    Column(modifier.fillMaxSize().background(WwmBackground)) {
        WwmTopBar(title = "어디서봐", leadingText = "‹", onLeadingClick = onBack)
        Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
            WwmCard(Modifier.fillMaxWidth()) {
                Column(
                    Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    Box(Modifier.background(WwmSoftIndigo, CircleShape).padding(14.dp)) {
                        Text("♧", color = WwmIndigo, fontSize = 24.sp)
                    }
                    Text("약속에 참여해요", color = WwmText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
                    Text(
                        "초대받은 방 코드와 사용할 닉네임을 입력해주세요.",
                        color = WwmMuted,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    OutlinedTextField(
                        value = roomCode,
                        onValueChange = { roomCode = it.uppercase() },
                        label = { Text("방 코드") },
                        placeholder = { Text("초대 코드 입력") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = { nickname = it },
                        label = { Text("어떤 이름으로 참여할까요?") },
                        placeholder = { Text("닉네임 입력") },
                        supportingText = { Text("로그인 없이 닉네임만 입력하면 참여할 수 있어요.") },
                        modifier = Modifier.fillMaxWidth(),
                    )
                    WwmPrimaryButton("참여하고 가능한 날짜 선택하기 →", { onJoin(roomCode, nickname) })
                }
            }
        }
    }
}
