package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.garam.whenwheremeet.platform.AuthSession
import com.garam.whenwheremeet.platform.PlatformBackHandler
import com.garam.whenwheremeet.presentation.component.MainBottomBar
import com.garam.whenwheremeet.presentation.component.MainTab
import com.garam.whenwheremeet.presentation.component.WwmBackground
import com.garam.whenwheremeet.presentation.component.WwmBorder
import com.garam.whenwheremeet.presentation.component.WwmCard
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmText
import com.garam.whenwheremeet.presentation.component.WwmTopBar

@Composable
fun MyPageScreen(
    authSession: AuthSession?,
    showAppleSignIn: Boolean,
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenCalendar: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTheme by remember { mutableStateOf(ScreenMode.Light) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf(SettingsDestination.Main) }
    val isAnonymous = authSession?.isAnonymous != false

    PlatformBackHandler(enabled = destination != SettingsDestination.Main) {
        destination = SettingsDestination.Main
    }

    Box(modifier.fillMaxSize().background(WwmBackground)) {
        when (destination) {
            SettingsDestination.Main -> {
                SettingsMainContent(
                    authSession = authSession,
                    isAnonymous = isAnonymous,
                    selectedMode = selectedTheme,
                    onOpenLogin = { showLoginSheet = true },
                    onOpenAccount = { destination = SettingsDestination.Account },
                    onOpenFeedback = onOpenFeedback,
                    onOpenScreenMode = { destination = SettingsDestination.ScreenMode },
                    onOpenTerms = onOpenTerms,
                    onOpenPrivacy = onOpenPrivacy,
                )
                MainBottomBar(
                    selectedTab = MainTab.MY_PAGE,
                    onHomeClick = onOpenHome,
                    onCalendarClick = onOpenCalendar,
                    onMyPageClick = {},
                    modifier = Modifier.align(Alignment.BottomCenter),
                )
            }
            SettingsDestination.Account -> {
                AccountSettingsContent(
                    authSession = authSession,
                    onBack = { destination = SettingsDestination.Main },
                    onSignOut = {
                        destination = SettingsDestination.Main
                        onSignOut()
                    },
                    onDeleteAccount = {
                        destination = SettingsDestination.Main
                        onDeleteAccount()
                    },
                )
            }
            SettingsDestination.ScreenMode -> {
                ScreenModeSettingsContent(
                    selectedMode = selectedTheme,
                    onSelectMode = { selectedTheme = it },
                    onBack = { destination = SettingsDestination.Main },
                )
            }
        }
    }

    if (showLoginSheet) {
        SettingsLoginBottomSheet(
            showAppleSignIn = showAppleSignIn,
            onDismiss = { showLoginSheet = false },
            onGoogleSignIn = {
                showLoginSheet = false
                onGoogleSignIn()
            },
            onAppleSignIn = {
                showLoginSheet = false
                onAppleSignIn()
            },
        )
    }
}

private enum class SettingsDestination {
    Main,
    Account,
    ScreenMode,
}

private enum class ScreenMode(val label: String) {
    Light("라이트 모드"),
    Dark("다크 모드"),
}

@Composable
private fun SettingsMainContent(
    authSession: AuthSession?,
    isAnonymous: Boolean,
    selectedMode: ScreenMode,
    onOpenLogin: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenScreenMode: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        WwmTopBar(title = "설정", trailingText = "나")
        LazyColumn(
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 108.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                SettingsMenuItem(
                    title = "로그인 정보",
                    description = if (isAnonymous) "로그인이 필요해요" else authSession?.email ?: "이메일 정보가 없어요",
                    onClick = if (isAnonymous) onOpenLogin else onOpenAccount,
                )
            }
            item {
                SettingsMenuItem(
                    title = "피드백 보내기",
                    description = "의견을 남길 수 있는 폼으로 이동해요",
                    onClick = onOpenFeedback,
                )
            }
            item {
                SettingsMenuItem(
                    title = "화면 모드",
                    description = selectedMode.label,
                    onClick = onOpenScreenMode,
                )
            }
            item {
                SettingsMenuItem(
                    title = "이용 약관",
                    description = "웹 페이지로 이동해요",
                    onClick = onOpenTerms,
                )
            }
            item {
                SettingsMenuItem(
                    title = "개인정보처리방침",
                    description = "웹 페이지로 이동해요",
                    onClick = onOpenPrivacy,
                )
            }
        }
    }
}

@Composable
private fun AccountSettingsContent(
    authSession: AuthSession?,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    onDeleteAccount: () -> Unit,
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        WwmTopBar(
            title = "로그인 정보",
            leadingText = "<",
            onLeadingClick = onBack,
        )
        Column(
            Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 20.dp),
            verticalArrangement = Arrangement.SpaceBetween,
        ) {
            WwmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("이메일", color = WwmMuted, fontSize = 13.sp)
                    Text(authSession?.email ?: "이메일 정보가 없어요", color = WwmText, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                }
            }
            Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                SettingsActionButton(text = "로그아웃", onClick = onSignOut)
                SettingsActionButton(text = "회원 탈퇴", danger = true, onClick = { showDeleteConfirm = true })
            }
        }
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("회원 탈퇴") },
            text = { Text("계정을 삭제할까요? 이 작업은 되돌릴 수 없어요.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirm = false
                        onDeleteAccount()
                    },
                ) {
                    Text("탈퇴", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text("취소")
                }
            },
        )
    }
}

@Composable
private fun ScreenModeSettingsContent(
    selectedMode: ScreenMode,
    onSelectMode: (ScreenMode) -> Unit,
    onBack: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        WwmTopBar(
            title = "화면 모드",
            leadingText = "<",
            onLeadingClick = onBack,
        )
        Column(Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 20.dp)) {
            WwmCard(Modifier.fillMaxWidth()) {
                Column(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp)) {
                    Text("모드 선택", color = WwmText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Text("현재 UI는 라이트 모드로 표시돼요", color = WwmMuted, fontSize = 13.sp)
                    Spacer(Modifier.height(10.dp))
                    ScreenMode.entries.forEach { mode ->
                        Row(
                            Modifier.fillMaxWidth()
                                .selectable(
                                    selected = selectedMode == mode,
                                    onClick = { onSelectMode(mode) },
                                )
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(
                                selected = selectedMode == mode,
                                onClick = { onSelectMode(mode) },
                                colors = RadioButtonDefaults.colors(selectedColor = WwmIndigo),
                            )
                            Text(mode.label, color = WwmText, fontSize = 14.sp)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SettingsMenuItem(
    title: String,
    description: String,
    onClick: (() -> Unit)?,
) {
    WwmCard(Modifier.fillMaxWidth(), onClick = onClick) {
        Row(
            Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                Text(title, color = WwmText, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                Text(description, color = WwmMuted, fontSize = 13.sp)
            }
            if (onClick != null) {
                Text(">", color = WwmMuted, fontSize = 18.sp)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SettingsLoginBottomSheet(
    showAppleSignIn: Boolean,
    onDismiss: () -> Unit,
    onGoogleSignIn: () -> Unit,
    onAppleSignIn: () -> Unit,
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
        shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
    ) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Text("로그인하기", color = WwmText, fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
            Text("로그인하면 참여 중인 약속과 설정을 계정에 연결할 수 있어요.", color = WwmMuted, fontSize = 13.sp, lineHeight = 19.sp)
            LoginButton(text = "Google로 계속하기", icon = "G", onClick = onGoogleSignIn)
            if (showAppleSignIn) {
                LoginButton(text = "Apple로 계속하기", icon = "A", dark = true, onClick = onAppleSignIn)
            }
            Spacer(Modifier.height(14.dp))
        }
    }
}

@Composable
private fun SettingsActionButton(
    text: String,
    danger: Boolean = false,
    onClick: () -> Unit,
) {
    OutlinedButton(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth().height(48.dp),
        shape = RoundedCornerShape(8.dp),
        border = BorderStroke(1.dp, if (danger) Color(0xFFD32F2F) else WwmBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = if (danger) Color(0xFFD32F2F) else WwmText,
        ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
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
