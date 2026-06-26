package com.garam.whenwheremeet.presentation.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

enum class MainTab {
    HOME,
    CALENDAR,
    MY_PAGE,
}

@Composable
fun MainBottomBar(
    selectedTab: MainTab,
    onHomeClick: () -> Unit,
    onCalendarClick: () -> Unit,
    onMyPageClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier.fillMaxWidth().height(80.dp)
            .background(WwmNavBackground, RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
            .padding(horizontal = 24.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        MainBottomItem(BottomIcon.Home, "홈", selectedTab == MainTab.HOME, onHomeClick)
        MainBottomItem(BottomIcon.Calendar, "캘린더", selectedTab == MainTab.CALENDAR, onCalendarClick)
        MainBottomItem(BottomIcon.Person, "내 정보", selectedTab == MainTab.MY_PAGE, onMyPageClick)
    }
}

@Composable
private fun MainBottomItem(icon: BottomIcon, label: String, selected: Boolean, onClick: () -> Unit) {
    val contentColor = if (selected) Color.White else WwmMuted
    Box(
        Modifier
            .width(92.dp)
            .height(60.dp)
            .semantics {
                role = Role.Tab
                contentDescription = label
                this.selected = selected
            }
            .background(
                color = if (selected) WwmIndigo else Color.Transparent,
                shape = RoundedCornerShape(999.dp),
            )
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
            BottomBarIcon(icon = icon, color = contentColor)
            Text(
                label,
                color = contentColor,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }
    }
}

private enum class BottomIcon {
    Home,
    Calendar,
    Person,
}

@Composable
private fun BottomBarIcon(icon: BottomIcon, color: Color) {
    Canvas(Modifier.size(30.dp).padding(bottom = 3.dp)) {
        val stroke = Stroke(
            width = 3.dp.toPx(),
            cap = StrokeCap.Square,
            join = StrokeJoin.Miter,
        )

        when (icon) {
            BottomIcon.Home -> {
                val roof = Path().apply {
                    moveTo(size.width * 0.2f, size.height * 0.48f)
                    lineTo(size.width * 0.5f, size.height * 0.2f)
                    lineTo(size.width * 0.8f, size.height * 0.48f)
                }
                val frame = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.45f)
                    lineTo(size.width * 0.25f, size.height * 0.84f)
                    lineTo(size.width * 0.42f, size.height * 0.84f)
                    lineTo(size.width * 0.42f, size.height * 0.63f)
                    lineTo(size.width * 0.58f, size.height * 0.63f)
                    lineTo(size.width * 0.58f, size.height * 0.84f)
                    lineTo(size.width * 0.75f, size.height * 0.84f)
                    lineTo(size.width * 0.75f, size.height * 0.45f)
                }
                drawPath(roof, color = color, style = stroke)
                drawPath(frame, color = color, style = stroke)
            }

            BottomIcon.Calendar -> {
                drawRoundRect(
                    color = color,
                    topLeft = Offset(size.width * 0.22f, size.height * 0.28f),
                    size = Size(size.width * 0.56f, size.height * 0.5f),
                    cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx()),
                    style = stroke,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.22f, size.height * 0.42f),
                    end = Offset(size.width * 0.78f, size.height * 0.42f),
                    strokeWidth = 3.dp.toPx(),
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.35f, size.height * 0.18f),
                    end = Offset(size.width * 0.35f, size.height * 0.34f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
                drawLine(
                    color = color,
                    start = Offset(size.width * 0.65f, size.height * 0.18f),
                    end = Offset(size.width * 0.65f, size.height * 0.34f),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            }

            BottomIcon.Person -> {
                drawCircle(
                    color = color,
                    radius = size.minDimension * 0.14f,
                    center = Offset(size.width * 0.5f, size.height * 0.27f),
                    style = stroke,
                )
                val shoulders = Path().apply {
                    moveTo(size.width * 0.25f, size.height * 0.78f)
                    lineTo(size.width * 0.25f, size.height * 0.68f)
                    quadraticTo(size.width * 0.5f, size.height * 0.55f, size.width * 0.75f, size.height * 0.68f)
                    lineTo(size.width * 0.75f, size.height * 0.78f)
                    close()
                }
                drawPath(shoulders, color = color, style = stroke)
            }
        }
    }
}
