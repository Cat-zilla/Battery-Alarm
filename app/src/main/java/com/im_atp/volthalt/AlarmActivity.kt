package com.im_atp.volthalt

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryAlert
import androidx.compose.material.icons.filled.BatteryFull
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.im_atp.volthalt.ui.theme.VoltHaltTheme

class AlarmActivity : ComponentActivity() {

    private var wakeLock: PowerManager.WakeLock? = null

    // Which alarm type this screen is currently showing.
    private var currentAlarmType = BatteryService.ALARM_TYPE_MAX

    companion object {
        private const val WAKE_LOCK_TIMEOUT_MS = 10 * 60 * 1000L  // 10 minutes
    }

    // BatteryService sends this when the alarm has been stopped (e.g. via the
    // notification "Stop Alarm" action), so we can dismiss ourselves.
    private val alarmStoppedReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == BatteryService.ACTION_ALARM_STOPPED) finish()
        }
    }

    // For max-battery alarms we watch for the charger being unplugged.
    // When that happens the alarm condition is resolved and we dismiss.
    private val batteryStateReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action != Intent.ACTION_BATTERY_CHANGED) return
            if (currentAlarmType != BatteryService.ALARM_TYPE_MAX) return

            val status     = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            val isCharging = status == BatteryManager.BATTERY_STATUS_CHARGING ||
                             status == BatteryManager.BATTERY_STATUS_FULL

            if (!isCharging) finish()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Make sure the screen turns on and the alarm shows over the lock screen.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON   or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
            )
        }

        // Wake lock keeps the CPU running while the alarm screen is visible.
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "VoltHalt::AlarmWakeLock"
        ).apply { acquire(WAKE_LOCK_TIMEOUT_MS) }

        currentAlarmType = intent?.getStringExtra(BatteryService.EXTRA_ALARM_TYPE) ?: BatteryService.ALARM_TYPE_MAX

        // ACTION_BATTERY_CHANGED is sticky, so registering immediately delivers
        // the current state without waiting for the next broadcast.
        registerReceiver(batteryStateReceiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))

        val alarmFilter = IntentFilter(BatteryService.ACTION_ALARM_STOPPED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(alarmStoppedReceiver, alarmFilter, RECEIVER_NOT_EXPORTED)
        } else {
            registerReceiver(alarmStoppedReceiver, alarmFilter)
        }

        val alarmType = intent?.getStringExtra(BatteryService.EXTRA_ALARM_TYPE)
            ?: BatteryService.ALARM_TYPE_MAX

        showAlarmUi(alarmType)
    }

    // Called when the activity is already running and a new alarm intent arrives.
    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        setIntent(intent)
        val alarmType = intent?.getStringExtra(BatteryService.EXTRA_ALARM_TYPE)
            ?: BatteryService.ALARM_TYPE_MAX
        showAlarmUi(alarmType)
    }

    private fun showAlarmUi(alarmType: String) {
        currentAlarmType = alarmType
        setContent {
            VoltHaltTheme(darkTheme = true) {
                AlarmScreen(
                    alarmType = alarmType,
                    onStopAlarm = {
                        stopAlarmInService()
                        finish()
                    }
                )
            }
        }
    }

    private fun stopAlarmInService() {
        startService(
            Intent(this, BatteryService::class.java).apply {
                action = BatteryService.ACTION_STOP_ALARM
            }
        )
    }

    // Prevent the back button from dismissing the alarm — the user must press Stop.
    @Deprecated("Deprecated in Java")
    override fun onBackPressed() { /* intentionally empty */ }

    override fun onDestroy() {
        super.onDestroy()
        try { unregisterReceiver(alarmStoppedReceiver) } catch (_: Exception) {}
        try { unregisterReceiver(batteryStateReceiver) } catch (_: Exception) {}
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }
}

@Composable
private fun AlarmScreen(
    alarmType: String,
    onStopAlarm: () -> Unit
) {
    val isMaxBattery = alarmType == BatteryService.ALARM_TYPE_MAX

    val context = LocalContext.current
    var batteryLevel by remember { mutableIntStateOf(-1) }
    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(context: Context?, intent: Intent?) {
                val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                if (level >= 0 && scale > 0) batteryLevel = (level * 100 / scale).coerceIn(0, 100)
            }
        }
        context.registerReceiver(receiver, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        onDispose { context.unregisterReceiver(receiver) }
    }

    Box(Modifier.fillMaxSize().background(Color(0xFF101010)).systemBarsPadding(), contentAlignment = Alignment.Center) {
        Column(
            Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(horizontal = 28.dp, vertical = 36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text("VOLTHALT  /  BATTERY ALERT", color = Color(0xFFAAAAAA), style = MaterialTheme.typography.labelLarge, letterSpacing = 1.5.sp, textAlign = TextAlign.Center)
            Spacer(Modifier.height(48.dp))
            Box(Modifier.size(96.dp).background(Color(0xFF292929), CircleShape), contentAlignment = Alignment.Center) {
                Icon(if (isMaxBattery) Icons.Default.BatteryFull else Icons.Default.BatteryAlert, contentDescription = null, tint = Color.White, modifier = Modifier.size(48.dp))
            }
            Spacer(Modifier.height(28.dp))
            Text(if (batteryLevel >= 0) "$batteryLevel%" else "—", fontSize = 72.sp, lineHeight = 80.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(Modifier.height(12.dp))
            Text(if (isMaxBattery) "Charge target reached" else "Low battery threshold reached", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold, color = Color.White, textAlign = TextAlign.Center)
            Spacer(Modifier.height(12.dp))
            Text(if (isMaxBattery) "Unplug your charger or stop the alarm." else "Connect your charger or stop the alarm.", style = MaterialTheme.typography.bodyLarge, color = Color(0xFFBDBDBD), textAlign = TextAlign.Center)
            Spacer(Modifier.height(56.dp))
            Button(onClick = onStopAlarm, modifier = Modifier.fillMaxWidth().heightIn(min = 60.dp), shape = RoundedCornerShape(16.dp), colors = ButtonDefaults.buttonColors(containerColor = Color.White, contentColor = Color(0xFF101010))) {
                Text("Stop alarm", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleMedium)
            }
        }
    }
}
