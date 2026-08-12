package com.garam.whenwheremeet.presentation.screen

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
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
import com.garam.whenwheremeet.presentation.component.WwmDesktopBreakpoint
import com.garam.whenwheremeet.presentation.component.WwmDesktopContentMaxWidth
import com.garam.whenwheremeet.presentation.component.WwmDesktopTopBar
import com.garam.whenwheremeet.presentation.component.WwmError
import com.garam.whenwheremeet.presentation.component.WwmIndigo
import com.garam.whenwheremeet.presentation.component.WwmMuted
import com.garam.whenwheremeet.presentation.component.WwmSoftIndigo
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
    onCreateRoom: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTheme by remember { mutableStateOf(ScreenMode.Light) }
    var showLoginSheet by remember { mutableStateOf(false) }
    var destination by remember { mutableStateOf(SettingsDestination.Main) }
    val isAnonymous = authSession?.isAnonymous != false

    PlatformBackHandler(enabled = destination != SettingsDestination.Main) {
        destination = SettingsDestination.Main
    }

    BoxWithConstraints(modifier.fillMaxSize().background(WwmBackground)) {
        val isDesktop = maxWidth >= WwmDesktopBreakpoint
        when (destination) {
            SettingsDestination.Main -> {
                if (isDesktop) {
                    DesktopSettingsMainContent(
                        authSession = authSession,
                        isAnonymous = isAnonymous,
                        selectedMode = selectedTheme,
                        onOpenLogin = { showLoginSheet = true },
                        onOpenAccount = { destination = SettingsDestination.Account },
                        onOpenFeedback = onOpenFeedback,
                        onOpenScreenMode = { destination = SettingsDestination.ScreenMode },
                        onOpenTerms = onOpenTerms,
                        onOpenPrivacy = onOpenPrivacy,
                        onOpenHome = onOpenHome,
                        onOpenCalendar = onOpenCalendar,
                        onCreateRoom = onCreateRoom,
                    )
                } else {
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

@Composable
private fun DesktopSettingsMainContent(
    authSession: AuthSession?,
    isAnonymous: Boolean,
    selectedMode: ScreenMode,
    onOpenLogin: () -> Unit,
    onOpenAccount: () -> Unit,
    onOpenFeedback: () -> Unit,
    onOpenScreenMode: () -> Unit,
    onOpenTerms: () -> Unit,
    onOpenPrivacy: () -> Unit,
    onOpenHome: () -> Unit,
    onOpenCalendar: () -> Unit,
    onCreateRoom: () -> Unit,
) {
    Column(Modifier.fillMaxSize()) {
        WwmDesktopTopBar(
            selectedTab = MainTab.MY_PAGE,
            onHomeClick = onOpenHome,
            onCalendarClick = onOpenCalendar,
            onMyPageClick = {},
            onCreateRoom = onCreateRoom,
        )
        Row(
            Modifier.weight(1f).fillMaxWidth().widthIn(max = WwmDesktopContentMaxWidth)
                .align(Alignment.CenterHorizontally).padding(horizontal = 24.dp, vertical = 64.dp),
            horizontalArrangement = Arrangement.spacedBy(64.dp),
            verticalAlignment = Alignment.Top,
        ) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                WwmCard(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(28.dp), verticalArrangement = Arrangement.spacedBy(22.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                            Box(
                                Modifier.size(92.dp).background(WwmSoftIndigo, RoundedCornerShape(46.dp)),
                                contentAlignment = Alignment.Center,
                            ) {
                                Text(
                                    if (isAnonymous) "게" else (authSession?.email?.take(1)?.uppercase() ?: "나"),
                                    color = WwmIndigo,
                                    fontSize = 30.sp,
                                    fontWeight = FontWeight.Bold,
                                )
                            }
                            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(if (isAnonymous) "게스트 사용자" else "내 계정", color = WwmText, fontSize = 24.sp, fontWeight = FontWeight.Bold)
                                Text(
                                    if (isAnonymous) "로그인하면 약속을 안전하게 보관해요" else authSession?.email ?: "이메일 정보가 없어요",
                                    color = WwmMuted,
                                )
                            }
                        }
                        OutlinedButton(
                            onClick = if (isAnonymous) onOpenLogin else onOpenAccount,
                            modifier = Modifier.fillMaxWidth().height(52.dp),
                            shape = RoundedCornerShape(14.dp),
                            border = BorderStroke(1.dp, WwmIndigo),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = WwmIndigo),
                        ) { Text(if (isAnonymous) "로그인" else "프로필 수정", fontWeight = FontWeight.SemiBold) }
                    }
                }
                DesktopSettingsGroup("계정 관리") {
                    SettingsMenuItem(
                        title = "로그인 정보",
                        description = if (isAnonymous) "로그인이 필요해요" else authSession?.email ?: "이메일 정보가 없어요",
                        onClick = if (isAnonymous) onOpenLogin else onOpenAccount,
                    )
                }
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(28.dp)) {
                DesktopSettingsGroup("앱 설정") {
                    SettingsMenuItem("알림 설정", "이벤트 초대 및 업데이트 알림", onClick = {})
                    SettingsDivider()
                    SettingsMenuItem("화면 모드", selectedMode.label, onClick = onOpenScreenMode)
                }
                DesktopSettingsGroup("고객 지원") {
                    SettingsMenuItem("피드백 보내기", "서비스에 대한 의견을 들려주세요", onClick = onOpenFeedback)
                    SettingsDivider()
                    SettingsMenuItem("이용 약관", "약관 내용을 확인해요", onClick = onOpenTerms)
                    SettingsDivider()
                    SettingsMenuItem("개인정보처리방침", "개인정보 보호 원칙을 확인해요", onClick = onOpenPrivacy)
                }
            }
        }
    }
}

@Composable
private fun DesktopSettingsGroup(title: String, content: @Composable () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(title, color = WwmText, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        HorizontalDivider(color = WwmBorder)
        Column(content = { content() })
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
            verticalArrangement = Arrangement.spacedBy(18.dp),
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(Color.White, RoundedCornerShape(18.dp))
                        .padding(18.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    Box(
                        modifier = Modifier
                            .background(WwmSoftIndigo, RoundedCornerShape(16.dp))
                            .padding(horizontal = 15.dp, vertical = 12.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = if (isAnonymous) "게" else (authSession?.email?.take(1)?.uppercase() ?: "나"),
                            color = WwmIndigo,
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                    Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Text(
                            text = if (isAnonymous) "게스트로 이용 중" else "내 계정",
                            color = WwmText,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                        Text(
                            text = if (isAnonymous) "로그인하면 약속을 안전하게 보관해요" else authSession?.email ?: "이메일 정보가 없어요",
                            color = WwmMuted,
                            fontSize = 13.sp,
                        )
                    }
                    Text(
                        text = if (isAnonymous) "로그인" else ">",
                        color = WwmIndigo,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.clickable(onClick = if (isAnonymous) onOpenLogin else onOpenAccount),
                    )
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("설정", color = WwmText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)),
                    ) {
                        SettingsMenuItem(
                            title = "로그인 정보",
                            description = if (isAnonymous) "로그인이 필요해요" else authSession?.email ?: "이메일 정보가 없어요",
                            onClick = if (isAnonymous) onOpenLogin else onOpenAccount,
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            title = "피드백 보내기",
                            description = "서비스에 대한 의견을 들려주세요",
                            onClick = onOpenFeedback,
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            title = "화면 모드",
                            description = selectedMode.label,
                            onClick = onOpenScreenMode,
                        )
                    }
                }
            }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("서비스 정보", color = WwmText, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Column(
                        modifier = Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(18.dp)),
                    ) {
                        SettingsMenuItem(
                            title = "이용 약관",
                            description = "약관 내용을 확인해요",
                            onClick = onOpenTerms,
                        )
                        SettingsDivider()
                        SettingsMenuItem(
                            title = "개인정보처리방침",
                            description = "개인정보 보호 원칙을 확인해요",
                            onClick = onOpenPrivacy,
                        )
                    }
                }
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
                    Text("탈퇴", color = WwmError)
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
    Row(
        Modifier.fillMaxWidth()
            .then(if (onClick == null) Modifier else Modifier.clickable(onClick = onClick))
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, color = WwmText, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            Text(description, color = WwmMuted, fontSize = 13.sp)
        }
        if (onClick != null) {
            Text(">", color = WwmMuted, fontSize = 18.sp)
        }
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        modifier = Modifier.padding(start = 18.dp),
        thickness = 1.dp,
        color = WwmBorder,
    )
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
        border = BorderStroke(1.dp, if (danger) WwmError else WwmBorder),
        colors = ButtonDefaults.outlinedButtonColors(
            containerColor = Color.White,
            contentColor = if (danger) WwmError else WwmText,
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
