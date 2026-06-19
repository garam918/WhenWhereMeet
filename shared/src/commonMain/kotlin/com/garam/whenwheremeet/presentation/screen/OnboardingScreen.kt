package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMint
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmNavBackground
import com.garam.whenwheremeet.presentation.component.WwmPrimaryButton
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmText

private const val TermsUrl = "https://whenwheremeet.app/terms"
private const val PrivacyUrl = "https://whenwheremeet.app/privacy"

@Composable
fun OnboardingScreen(
    showAppleSignIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
    onStartWithoutLogin: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var page by remember { mutableIntStateOf(0) }
    var showLoginSheet by remember { mutableStateOf(false) }

    Box(modifier.fillMaxSize().background(WwmBackground)) {
        Column(Modifier.fillMaxSize().padding(horizontal = 24.dp, vertical = 20.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(if (page == 0) "온보딩 - 날짜 조율" else "온보딩 - 장소 추천", color = WwmMuted, fontSize = 13.sp)
                Text(
                    "건너뛰기",
                    color = WwmIndigo,
                    fontSize = 12.sp,
                    modifier = Modifier.clickable { showLoginSheet = true },
                )
            }
            Spacer(Modifier.height(34.dp))
            when (page) {
                0 -> DateOnboardingPage()
                else -> PlaceOnboardingPage(onBack = { page = 0 })
            }
            Spacer(Modifier.weight(1f))
            OnboardingDots(page)
            Spacer(Modifier.height(14.dp))
            WwmPrimaryButton(
                text = if (page == 0) "다음" else "시작하기",
                onClick = {
                    if (page == 0) page = 1 else showLoginSheet = true
                },
            )
        }
    }

    if (showLoginSheet) {
        LoginBottomSheet(
            showAppleSignIn = showAppleSignIn,
            onDismiss = { showLoginSheet = false },
            onGoogleSignIn = onGoogleSignIn,
            onAppleSignIn = onAppleSignIn,
            onStartWithoutLogin = onStartWithoutLogin,
            onOpenTerms = { onOpenUrl(TermsUrl) },
            onOpenPrivacy = { onOpenUrl(PrivacyUrl) },
        )
    }
}

@Composable
private fun DateOnboardingPage() {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        CalendarMock()
        Spacer(Modifier.height(88.dp))
        Text("가능한 날만\n고르면 끝", color = WwmText, fontSize = 24.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.Center)
        Spacer(Modifier.height(18.dp))
        Text(
            "친구, 스터디, 회사 약속도 캘린더에서\n가능한 날짜를 모아 가장 좋은 날을 쉽게\n찾을 수 있어요.",
            color = WwmMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
    }
}

@Composable
private fun PlaceOnboardingPage(onBack: () -> Unit) {
    Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            Text("<", color = WwmText, fontSize = 24.sp, modifier = Modifier.clickable(onClick = onBack))
        }
        Box(Modifier.fillMaxWidth().height(260.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(210.dp).border(1.dp, WwmSoftIndigo, CircleShape))
            Box(Modifier.size(150.dp).border(1.dp, WwmSoftIndigo, CircleShape))
            WwmCard(Modifier.width(210.dp)) {
                Column(Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("추천 위치 1위", color = Color.White, fontSize = 11.sp, modifier = Modifier.background(Color(0xFF00714D), RoundedCornerShape(99.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("신도림역", color = WwmText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                    Text("1호선, 2호선 환승", color = WwmMuted, fontSize = 12.sp)
                    Spacer(Modifier.height(14.dp))
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceAround) {
                        TravelMetric("평균", "35분")
                        TravelMetric("최대", "45분")
                    }
                }
            }
        }
        Spacer(Modifier.height(30.dp))
        Text("장소도 공정하게 추천해요", color = WwmText, fontSize = 23.sp, fontWeight = FontWeight.SemiBold)
        Spacer(Modifier.height(16.dp))
        Text(
            "각자의 출발 위치와 이동수단을 기준으로\n모두에게 덜 불편한 만남 장소를 추천해요.",
            color = WwmMuted,
            fontSize = 14.sp,
            lineHeight = 21.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(20.dp))
        Row(
            Modifier.fillMaxWidth().background(Color(0xFFF0F3FF), RoundedCornerShape(8.dp)).padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("i", color = WwmIndigo, fontWeight = FontWeight.Bold)
            Spacer(Modifier.width(8.dp))
            Text("정확한 위치는 다른 참여자에게 공개되지 않아요.", color = WwmMuted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun CalendarMock() {
    WwmCard(Modifier.width(250.dp)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text("11월", color = WwmText, fontWeight = FontWeight.Bold)
                Text("2, 6월 중 가장 가능", color = Color(0xFF00714D), fontSize = 11.sp, modifier = Modifier.background(WwmMint, RoundedCornerShape(99.dp)).padding(horizontal = 10.dp, vertical = 4.dp))
            }
            listOf("일", "월", "화", "수", "목", "금", "토").chunked(7).forEach { week ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    week.forEach { Text(it, color = WwmMuted, fontSize = 10.sp, modifier = Modifier.width(24.dp), textAlign = TextAlign.Center) }
                }
            }
            val days = listOf("29", "30", "1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12", "13", "14", "15", "16", "17", "18")
            days.chunked(7).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    row.forEach { day ->
                        val selected = day in listOf("8", "14", "15")
                        Box(
                            Modifier.size(24.dp).background(if (selected) WwmIndigo else Color.Transparent, CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(day, color = if (selected) Color.White else WwmText, fontSize = 11.sp)
                        }
                    }
                    repeat(7 - row.size) { Spacer(Modifier.width(24.dp)) }
                }
            }
        }
    }
}

@Composable
private fun TravelMetric(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(label, color = WwmMuted, fontSize = 11.sp)
        Text(value, color = WwmText, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun OnboardingDots(page: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(2) { index ->
            Box(
                Modifier.size(width = if (page == index) 18.dp else 6.dp, height = 6.dp)
                    .background(if (page == index) WwmIndigo else WwmNavBackground, RoundedCornerShape(99.dp)),
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LoginBottomSheet(
    showAppleSignIn: Boolean,
    onDismiss: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
    onStartWithoutLogin: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("모임픽 시작하기", color = WwmText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("로그인하면 참여 중인 약속과 자주 쓰는 출발 위치를 안전하게 저장할 수 있어요.", color = WwmMuted, fontSize = 13.sp, lineHeight = 19.sp)
            LoginButton(text = "Google로 계속하기", icon = "G", onClick = onGoogleSignIn)
            if (showAppleSignIn) {
                LoginButton(text = "Apple로 계속하기", icon = "A", dark = true, onClick = onAppleSignIn)
            }
            Text(
                "익명으로 시작하기",
                color = WwmIndigo,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.align(Alignment.CenterHorizontally).clickable(onClick = onStartWithoutLogin).padding(vertical = 6.dp),
            )
            Row(Modifier.fillMaxWidth().background(Color(0xFFF0F3FF), RoundedCornerShape(8.dp)).padding(12.dp)) {
                Text("i", color = WwmIndigo, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(8.dp))
                Text("출발 위치는 이동시간 계산에만 사용되며, 다른 참여자에게 정확한 위치가 공개되지 않아요.", color = WwmMuted, fontSize = 12.sp, lineHeight = 17.sp)
            }
            Text(
                "계속 진행하면 개인정보 처리방침 및 이용약관에 동의하는 것으로 간주됩니다.",
                color = WwmMuted,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                Text("개인정보 처리방침", color = WwmIndigo, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onOpenPrivacy))
                Text("  ·  ", color = WwmMuted, fontSize = 11.sp)
                Text("이용약관", color = WwmIndigo, fontSize = 11.sp, modifier = Modifier.clickable(onClick = onOpenTerms))
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun LoginButton(text: String, icon: String, dark: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(44.dp),
        shape = RoundedCornerShape(999.dp),
        border = BorderStroke(1.dp, if (dark) Color.Black else WwmBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (dark) Color.Black else Color.White,
            contentColor = if (dark) Color.White else WwmText,
        ),
    ) {
        Text(icon, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(8.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
    }
}
