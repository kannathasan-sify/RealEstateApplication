package com.realestate.app.ui.booking

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.realestate.app.ui.components.RealEstateFilterChip
import com.realestate.app.ui.theme.*
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun BookingScreen(
    viewModel: BookingViewModel,
    propertyId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
) {
    val selectedDate by viewModel.selectedDate.collectAsState()
    val selectedTime by viewModel.selectedTime.collectAsState()
    val message      by viewModel.message.collectAsState()
    val state        by viewModel.state.collectAsState()

    LaunchedEffect(state) {
        if (state is BookingState.Success) onSuccess()
    }

    // ── Date picker dialog state ─────────────────────────────────────────────
    var showDatePicker by remember { mutableStateOf(false) }
    val datePickerState = rememberDatePickerState(
        // Default to today
        initialSelectedDateMillis = System.currentTimeMillis(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long): Boolean {
                // Allow today and future dates only
                val today = System.currentTimeMillis() - (System.currentTimeMillis() % 86_400_000L)
                return utcTimeMillis >= today
            }
        }
    )

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            // Format to YYYY-MM-DD in UTC
                            val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US).apply {
                                timeZone = TimeZone.getTimeZone("UTC")
                            }
                            viewModel.selectedDate.value = sdf.format(Date(millis))
                        }
                        showDatePicker = false
                    }
                ) { Text("OK", color = PrimaryRed, fontWeight = FontWeight.SemiBold) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel", color = TextSecondary)
                }
            },
        ) {
            DatePicker(
                state  = datePickerState,
                colors = DatePickerDefaults.colors(
                    selectedDayContainerColor   = PrimaryRed,
                    todayDateBorderColor        = PrimaryRed,
                    todayContentColor           = PrimaryRed,
                ),
            )
        }
    }

    val timeSlots = listOf("09:00", "10:00", "11:00", "12:00", "13:00", "14:00", "15:00", "16:00", "17:00")

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Book a Visit", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            )
        },
        bottomBar = {
            Surface(shadowElevation = 8.dp) {
                Button(
                    onClick  = { viewModel.book(propertyId) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .height(52.dp),
                    colors   = ButtonDefaults.buttonColors(containerColor = PrimaryRed),
                    shape    = RoundedCornerShape(12.dp),
                    enabled  = state !is BookingState.Loading,
                ) {
                    if (state is BookingState.Loading) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(20.dp))
                    } else {
                        Text("Confirm Visit", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                    }
                }
            }
        },
        containerColor = Color.White,
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
        ) {

            // ── Date selection ───────────────────────────────────────────────
            Column {
                Row(
                    verticalAlignment      = Alignment.CenterVertically,
                    horizontalArrangement  = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.CalendarToday, null, tint = PrimaryRed, modifier = Modifier.size(20.dp))
                    Text("Select Date", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(10.dp))

                // Tappable date card that opens the picker
                Card(
                    modifier  = Modifier
                        .fillMaxWidth()
                        .clickable { showDatePicker = true },
                    shape     = RoundedCornerShape(10.dp),
                    colors    = CardDefaults.cardColors(containerColor = Color.White),
                    border    = BorderStroke(1.dp, if (selectedDate.isBlank()) BorderColor else PrimaryRed),
                    elevation = CardDefaults.cardElevation(0.dp),
                ) {
                    Row(
                        modifier              = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 16.dp),
                        verticalAlignment     = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        if (selectedDate.isBlank()) {
                            Text(
                                "Tap to choose a date",
                                color    = TextSecondary,
                                fontSize = 15.sp,
                            )
                        } else {
                            // Show human-readable date
                            val displayDate = runCatching {
                                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
                                val outSdf = SimpleDateFormat("EEE, d MMM yyyy", Locale.US)
                                outSdf.format(sdf.parse(selectedDate)!!)
                            }.getOrDefault(selectedDate)

                            Text(
                                displayDate,
                                color      = TextPrimary,
                                fontSize   = 15.sp,
                                fontWeight = FontWeight.Medium,
                            )
                        }
                        Icon(
                            Icons.Filled.CalendarToday,
                            null,
                            tint     = PrimaryRed,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }

            // ── Time slot selection ──────────────────────────────────────────
            Column {
                Row(
                    verticalAlignment     = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Icon(Icons.Filled.Schedule, null, tint = PrimaryRed, modifier = Modifier.size(20.dp))
                    Text("Select Time", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                }
                Spacer(Modifier.height(10.dp))
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement   = Arrangement.spacedBy(8.dp),
                ) {
                    timeSlots.forEach { slot ->
                        RealEstateFilterChip(
                            label    = slot,
                            selected = selectedTime == slot,
                            onClick  = { viewModel.selectedTime.value = slot },
                        )
                    }
                }
            }

            // ── Optional message ─────────────────────────────────────────────
            Column {
                Text("Message (Optional)", fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = TextPrimary)
                Spacer(Modifier.height(8.dp))
                OutlinedTextField(
                    value         = message,
                    onValueChange = { viewModel.message.value = it },
                    label         = { Text("Add a message for the agent") },
                    modifier      = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape    = RoundedCornerShape(10.dp),
                    maxLines = 5,
                    colors   = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor   = PrimaryRed,
                        unfocusedBorderColor = BorderColor,
                    ),
                )
            }

            if (state is BookingState.Error) {
                Card(
                    shape  = RoundedCornerShape(8.dp),
                    colors = CardDefaults.cardColors(containerColor = OnboardingBlob),
                ) {
                    Text(
                        text     = (state as BookingState.Error).message,
                        color    = PrimaryRed,
                        fontSize = 13.sp,
                        modifier = Modifier.padding(12.dp),
                    )
                }
            }
        }
    }
}
