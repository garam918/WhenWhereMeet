package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
import com.garam.whenwheremeet.presentation.component.WwmSurface
import com.garam.whenwheremeet.presentation.component.WwmText
import org.jetbrains.compose.resources.painterResource
import whenwheremeet.shared.generated.resources.Res
import whenwheremeet.shared.generated.resources.app_icon

private const val TermsUrl = "https://whenwheremeet-legal.web.app/terms/"
private const val PrivacyUrl = "https://whenwheremeet-legal.web.app/privacy/"

@Composable
fun OnboardingScreen(
    showGoogleSignIn: Boolean,
    showAppleSignIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
    onOpenUrl: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier.fillMaxSize().background(WwmBackground).verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp, vertical = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text("언제어디", color = WwmIndigo, fontSize = 19.sp, fontWeight = FontWeight.Bold)
        Spacer(Modifier.height(44.dp))
        Image(
            painter = painterResource(Res.drawable.app_icon),
            contentDescription = "언제어디 앱 아이콘",
            modifier = Modifier.size(68.dp),
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.height(24.dp))
        Text(
            "날짜부터 장소까지,\n한 번에 정해요",
            color = WwmText,
            fontSize = 29.sp,
            lineHeight = 38.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(10.dp))
        Text(
            "여러 사람의 시간을 모으고\n모두에게 공정한 장소를 찾아드려요.",
            color = WwmMuted,
            fontSize = 15.sp,
            lineHeight = 22.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(32.dp))
        Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            OnboardingBenefit("1", "날짜 정하기", "가능한 날만 고르면 가장 좋은 날짜를 추천해요.")
            OnboardingBenefit("2", "중간 지역 찾기", "각자의 예상 이동시간을 비교해 공정하게 골라요.")
            OnboardingBenefit("3", "장소 투표", "후보를 함께 보고 투표해 최종 장소를 확정해요.")
        }
        Spacer(Modifier.height(36.dp))
        if (showAppleSignIn) {
            LoginButton(text = "Apple로 계속하기", icon = "●", dark = true, onClick = onAppleSignIn)
        }
        if (showAppleSignIn && showGoogleSignIn) {
            Spacer(Modifier.height(10.dp))
        }
        if (showGoogleSignIn) {
            LoginButton(text = "Google로 계속하기", icon = "G", onClick = onGoogleSignIn)
        }
        Spacer(Modifier.height(14.dp))
        Text(
            "약속을 안전하게 보관하고 모든 기기에서 불러오려면 로그인이 필요해요.",
            color = WwmMuted,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
        )
        Spacer(Modifier.height(18.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
            Text("개인정보처리방침", color = WwmMuted, fontSize = 11.sp, modifier = Modifier.clickable { onOpenUrl(PrivacyUrl) })
            Text("  ·  ", color = WwmMuted, fontSize = 11.sp)
            Text("이용약관", color = WwmMuted, fontSize = 11.sp, modifier = Modifier.clickable { onOpenUrl(TermsUrl) })
        }
    }
}

@Composable
private fun OnboardingBenefit(number: String, title: String, description: String) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(Modifier.size(40.dp).background(WwmSoftIndigo, CircleShape), contentAlignment = Alignment.Center) {
            Text(number, color = WwmIndigo, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
            Text(title, color = WwmText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = WwmMuted, fontSize = 13.sp, lineHeight = 19.sp)
        }
    }
}

@Composable
private fun LoginButton(text: String, icon: String, dark: Boolean = false, onClick: () -> Unit) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(52.dp),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, WwmBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = if (dark) Color.Black else WwmSurface,
            contentColor = if (dark) Color.White else WwmText,
        ),
    ) {
        Text(icon, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(9.dp))
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
