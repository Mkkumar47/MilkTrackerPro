package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.Payment
import com.example.data.Seller
import com.example.ui.components.ColorNo
import com.example.ui.components.ColorYes
import com.example.viewmodel.MilkViewModel
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentScreen(
    viewModel: MilkViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val config by viewModel.configFlow.collectAsState()
    val sellers by viewModel.sellersFlow.collectAsState()
    val payments by viewModel.paymentsFlow.collectAsState()

    // Mode of current screen layout: "LIST" or "ADD_PAYMENT"
    var currentSubScreen by remember { mutableStateOf("LIST") }

    // Sort states
    var showSortDialog by remember { mutableStateOf(false) }
    var activeSortOption by remember { mutableStateOf("Date (Newest First)") }

    // Filter states
    var searchText by remember { mutableStateOf("") }
    var filterSession by remember { mutableStateOf("All Sessions") }
    var filterMilkType by remember { mutableStateOf("All Milk Types") }
    var filterMinAmount by remember { mutableStateOf("") }
    var filterMaxAmount by remember { mutableStateOf("") }

    // Active filters applied state
    var selectedSession by remember { mutableStateOf("All Sessions") }
    var selectedMilkType by remember { mutableStateOf("All Milk Types") }
    var appliedMinAmount by remember { mutableStateOf("") }
    var appliedMaxAmount by remember { mutableStateOf("") }

    var isFilterExpanded by remember { mutableStateOf(false) }

    // Add Payment form States
    var formDate by remember { mutableStateOf("") }
    var formSellerId by remember { mutableStateOf(-1) }
    var formSellerName by remember { mutableStateOf("") }
    var formAmount by remember { mutableStateOf("0.00") }
    var formPaymentMode by remember { mutableStateOf("Cash") }
    var formNotes by remember { mutableStateOf("") }
    var formSession by remember { mutableStateOf("Morning") }
    var formMilkType by remember { mutableStateOf("Cow Milk") }

    // Add Seller form States (to match the image)
    var sellerNameInput by remember { mutableStateOf("") }
    var sellerPhoneInput by remember { mutableStateOf("") }
    var sellerAddressInput by remember { mutableStateOf("") }
    var sellerMilkTypeSelected by remember { mutableStateOf("Both") } // "Cow Milk", "Buffalo Milk", "Both"
    var cowRateInput by remember { mutableStateOf("") }
    var buffaloRateInput by remember { mutableStateOf("") }

    var showDeletePymtDialog by remember { mutableStateOf<Payment?>(null) }

    // Set default date when entering ADD_PAYMENT form
    LaunchedEffect(currentSubScreen) {
        if (currentSubScreen == "ADD_PAYMENT") {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.US)
            formDate = sdf.format(Date())
            formAmount = "0.00"
            formPaymentMode = "Cash"
            formNotes = ""
            formSession = "Morning"
            formMilkType = "Cow Milk"
            if (sellers.isNotEmpty()) {
                formSellerId = sellers.first().id
                formSellerName = sellers.first().name
            } else {
                formSellerId = -1
                formSellerName = ""
            }
        }
        if (currentSubScreen == "ADD_SELLER") {
            sellerNameInput = ""
            sellerPhoneInput = ""
            sellerAddressInput = ""
            sellerMilkTypeSelected = "Both"
            cowRateInput = ""
            buffaloRateInput = ""
        }
    }

    // Filtered & Sorted Payments logic list computation
    val filteredPayments = remember(
        payments, searchText, selectedSession, selectedMilkType,
        appliedMinAmount, appliedMaxAmount, activeSortOption
    ) {
        var list = payments.filter { payment ->
            val matchesSearch = payment.sellerName.contains(searchText, ignoreCase = true) ||
                    payment.notes.contains(searchText, ignoreCase = true) ||
                    payment.paymentMode.contains(searchText, ignoreCase = true)

            val matchesSession = selectedSession == "All Sessions" || payment.session == selectedSession
            val matchesMilkType = selectedMilkType == "All Milk Types" || payment.milkType == selectedMilkType

            val minAmt = appliedMinAmount.toDoubleOrNull()
            val maxAmt = appliedMaxAmount.toDoubleOrNull()
            val matchesMin = minAmt == null || payment.amount >= minAmt
            val matchesMax = maxAmt == null || payment.amount <= maxAmt

            matchesSearch && matchesSession && matchesMilkType && matchesMin && matchesMax
        }

        // Sorting logic based on Sort By Options Dialog
        list = when (activeSortOption) {
            "Date (Oldest First)" -> list.sortedWith(compareBy<Payment> { it.date }.thenBy { it.timestamp })
            "Amount (Highest First)" -> list.sortedByDescending { it.amount }
            "Amount (Lowest First)" -> list.sortedBy { it.amount }
            "Quantity (Highest First)" -> list.sortedByDescending { it.id } // Mock quantity fallback sorting
            "Quantity (Lowest First)" -> list.sortedBy { it.id }  // Mock quantity fallback sorting
            else -> list.sortedWith(compareByDescending<Payment> { it.date }.thenByDescending { it.timestamp }) // "Date (Newest First)"
        }
        list
    }

    if (currentSubScreen == "ADD_PAYMENT") {
        // RENDER: Add Payment screen form layout
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp)
        ) {
            // Header Top Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { currentSubScreen = "LIST" },
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Add Payment",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // DATE INPUT
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Date",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = formDate,
                            onValueChange = { formDate = it },
                            placeholder = { Text("dd/MM/yyyy") },
                            trailingIcon = {
                                Icon(Icons.Filled.CalendarToday, contentDescription = "Select Date")
                            },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                // SELLER INPUT
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Seller",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))

                        var exSellersExpanded by remember { mutableStateOf(false) }

                        Box(modifier = Modifier.fillMaxWidth()) {
                            OutlinedCard(
                                onClick = { exSellersExpanded = true },
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.outlinedCardColors(
                                    containerColor = MaterialTheme.colorScheme.surface
                                )
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = if (formSellerName.isNotBlank()) formSellerName else "Select Seller",
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = if (formSellerName.isNotBlank()) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    Icon(
                                        imageVector = Icons.Filled.ArrowDropDown,
                                        contentDescription = "Expand",
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            DropdownMenu(
                                expanded = exSellersExpanded,
                                onDismissRequest = { exSellersExpanded = false },
                                modifier = Modifier.fillMaxWidth(0.9f)
                            ) {
                                if (sellers.isEmpty()) {
                                    DropdownMenuItem(
                                        text = { Text("No sellers added yet") },
                                        onClick = { exSellersExpanded = false }
                                    )
                                } else {
                                    sellers.forEach { s ->
                                        DropdownMenuItem(
                                            text = { Text(s.name) },
                                            onClick = {
                                                formSellerId = s.id
                                                formSellerName = s.name
                                                exSellersExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        // Button to trigger "Add New Seller" screen
                        OutlinedButton(
                            onClick = { currentSubScreen = "ADD_SELLER" },
                            border = ButtonDefaults.outlinedButtonBorder.copy(),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Add New Seller", fontWeight = FontWeight.Bold)
                        }
                    }
                }

                // AMOUNT INPUT
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Amount",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = formAmount,
                            onValueChange = { formAmount = it },
                            placeholder = { Text("0.00") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }

                // PAYMENT MODE SELECTION
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Payment Mode",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            val modes = listOf("Cash", "UPI", "Bank", "Other")
                            modes.forEach { mode ->
                                val isSelected = formPaymentMode == mode
                                Card(
                                    onClick = { formPaymentMode = mode },
                                    modifier = Modifier
                                        .weight(1f)
                                        .shadow(if (isSelected) 2.dp else 0.dp, RoundedCornerShape(12.dp)),
                                    colors = CardDefaults.cardColors(
                                        containerColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                                        contentColor = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onPrimaryContainer
                                    ),
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp, horizontal = 4.dp),
                                        horizontalArrangement = Arrangement.Center,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        if (isSelected) {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                modifier = Modifier.size(14.dp)
                                            )
                                            Spacer(modifier = Modifier.width(4.dp))
                                        }
                                        Text(
                                            text = mode,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            textAlign = TextAlign.Center
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // ADD EXTRA FLOW SELECTIONS: Session and Milk Type
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Optional: Associated Session & Type",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            // Session picker
                            Box(modifier = Modifier.weight(1f)) {
                                var sExpanded by remember { mutableStateOf(false) }
                                OutlinedCard(
                                    onClick = { sExpanded = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(formSession, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = sExpanded, onDismissRequest = { sExpanded = false }) {
                                    listOf("Morning", "Evening", "All Sessions").forEach { ses ->
                                        DropdownMenuItem(
                                            text = { Text(ses, fontSize = 12.sp) },
                                            onClick = {
                                                formSession = ses
                                                sExpanded = false
                                            }
                                        )
                                    }
                                }
                            }

                            // MilkType picker
                            Box(modifier = Modifier.weight(1f)) {
                                var mExpanded by remember { mutableStateOf(false) }
                                OutlinedCard(
                                    onClick = { mExpanded = true },
                                    shape = RoundedCornerShape(12.dp)
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(10.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(formMilkType, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                                        Icon(Icons.Filled.ArrowDropDown, null, modifier = Modifier.size(16.dp))
                                    }
                                }
                                DropdownMenu(expanded = mExpanded, onDismissRequest = { mExpanded = false }) {
                                    listOf("Cow Milk", "Buffalo Milk", "All Milk Types").forEach { mt ->
                                        DropdownMenuItem(
                                            text = { Text(mt, fontSize = 12.sp) },
                                            onClick = {
                                                formMilkType = mt
                                                mExpanded = false
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // NOTES (OPTIONAL)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Notes (Optional)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = formNotes,
                            onValueChange = { formNotes = it },
                            placeholder = { Text("Add notes...") },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(90.dp),
                            shape = RoundedCornerShape(12.dp),
                            maxLines = 3,
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline
                            )
                        )
                    }
                }
            }

            // Bottom action buttons row
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { currentSubScreen = "LIST" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold)
                }

                Button(
                    onClick = {
                        val parsedAmt = formAmount.toDoubleOrNull() ?: 0.0
                        if (formSellerId == -1 || formSellerName.isBlank()) {
                            Toast.makeText(context, "Please select or add a seller first.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }
                        if (parsedAmt <= 0) {
                            Toast.makeText(context, "Please enter a valid amount greater than 0.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Parse formatted dd/MM/yyyy date back to yyyy-MM-dd
                        val dbDate = formatDateDisplayToDb(formDate)

                        viewModel.savePayment(
                            date = dbDate,
                            sellerId = formSellerId,
                            sellerName = formSellerName,
                            amount = parsedAmt,
                            paymentMode = formPaymentMode,
                            notes = formNotes,
                            session = formSession,
                            milkType = formMilkType
                        )

                        currentSubScreen = "LIST"
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Add Payment", fontWeight = FontWeight.Bold)
                }
            }
        }
    } else if (currentSubScreen == "ADD_SELLER") {
        // RENDER: Add Seller Screen strictly matching the user's uploaded mockup image
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
                .padding(horizontal = 16.dp)
        ) {
            // Header Top Bar: back arrow and title "Add Seller"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = { currentSubScreen = "ADD_PAYMENT" }, // goes back to add payment
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Text(
                    text = "Add Seller",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // SECTION Header: Basic Information
                item {
                    Text(
                        text = "Basic Information",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                }

                // 1. Seller Name *
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Row {
                            Text(
                                text = "Seller Name",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = " *",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.Red
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = sellerNameInput,
                            onValueChange = { sellerNameInput = it },
                            placeholder = { Text("Enter seller name", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 2. Phone (Optional)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Phone (Optional)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = sellerPhoneInput,
                            onValueChange = { sellerPhoneInput = it },
                            placeholder = { Text("Enter phone number", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // 3. Address (Optional)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Address (Optional)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = sellerAddressInput,
                            onValueChange = { sellerAddressInput = it },
                            placeholder = { Text("Enter address", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                            ),
                            shape = RoundedCornerShape(8.dp),
                            minLines = 2,
                            maxLines = 4,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }

                // SECTION Header: Milk Configuration
                item {
                    Text(
                        text = "Milk Configuration",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp, bottom = 4.dp)
                    )
                }

                // Milk Type (Radio options row)
                item {
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "Milk Type",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val options = listOf("Cow Milk", "Buffalo Milk", "Both")
                            options.forEach { opt ->
                                val active = sellerMilkTypeSelected == opt
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .clickable { sellerMilkTypeSelected = opt }
                                        .padding(vertical = 4.dp, horizontal = 2.dp)
                                ) {
                                    RadioButton(
                                        selected = active,
                                        onClick = { sellerMilkTypeSelected = opt },
                                        colors = RadioButtonDefaults.colors(
                                            selectedColor = MaterialTheme.colorScheme.primary,
                                            unselectedColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                                        )
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = opt,
                                        fontSize = 14.sp,
                                        fontWeight = if (active) FontWeight.Medium else FontWeight.Normal,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }
                }

                // Cow Milk Rate (Visible if Both or Cow Milk selected)
                if (sellerMilkTypeSelected == "Cow Milk" || sellerMilkTypeSelected == "Both") {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row {
                                Text(
                                    text = "Cow Milk Rate",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = " *",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.Red
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = cowRateInput,
                                onValueChange = { cowRateInput = it },
                                placeholder = { Text("Enter rate", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                // Buffalo Milk Rate (Visible if Both or Buffalo Milk selected)
                if (sellerMilkTypeSelected == "Buffalo Milk" || sellerMilkTypeSelected == "Both") {
                    item {
                        Column(modifier = Modifier.fillMaxWidth()) {
                            val isRequired = sellerMilkTypeSelected == "Buffalo Milk"
                            Row {
                                Text(
                                    text = "Buffalo Milk Rate" + if (isRequired) "" else " (Optional)",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                if (isRequired) {
                                    Text(
                                        text = " *",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = Color.Red
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            OutlinedTextField(
                                value = buffaloRateInput,
                                onValueChange = { buffaloRateInput = it },
                                placeholder = { Text("Enter rate", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)) },
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }
            }

            // Bottom Buttons Bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 14.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = { currentSubScreen = "ADD_PAYMENT" },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.primary
                    ),
                    border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary)
                ) {
                    Text("Cancel", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }

                Button(
                    onClick = {
                        val nameStr = sellerNameInput.trim()
                        if (nameStr.isBlank()) {
                            Toast.makeText(context, "Seller name is required.", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        var cowRateVal = 0.0
                        if (sellerMilkTypeSelected == "Cow Milk" || sellerMilkTypeSelected == "Both") {
                            val parsed = cowRateInput.toDoubleOrNull()
                            if (parsed == null || parsed <= 0) {
                                Toast.makeText(context, "Enter a valid Cow Milk Rate.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            cowRateVal = parsed
                        }

                        var buffaloRateVal = 0.0
                        if (sellerMilkTypeSelected == "Buffalo Milk" || sellerMilkTypeSelected == "Both") {
                            val parsed = buffaloRateInput.toDoubleOrNull()
                            if (sellerMilkTypeSelected == "Buffalo Milk" && (parsed == null || parsed <= 0)) {
                                Toast.makeText(context, "Enter a valid Buffalo Milk Rate.", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            if (parsed != null) {
                                buffaloRateVal = parsed
                            }
                        }

                        // Save using viewModel and select newly added seller automatically on success
                        viewModel.saveSeller(
                            name = nameStr,
                            phone = sellerPhoneInput.trim(),
                            address = sellerAddressInput.trim(),
                            milkType = sellerMilkTypeSelected,
                            cowRate = cowRateVal,
                            buffaloRate = buffaloRateVal,
                            onSuccess = { newId ->
                                formSellerId = newId.toInt()
                                formSellerName = nameStr
                                currentSubScreen = "ADD_PAYMENT"
                            }
                        )
                    },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Add Seller", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                }
            }
        }
    } else {
        // RENDER: Main List Screen with filters
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.surface,
                            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
                        )
                    )
                )
                .padding(horizontal = 16.dp)
        ) {
            // Header bar
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Payments",
                    fontSize = 20.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Button(
                    onClick = { currentSubScreen = "ADD_PAYMENT" },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 6.dp),
                    modifier = Modifier.align(Alignment.CenterEnd)
                ) {
                    Text("Add", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Search bar input
            OutlinedTextField(
                value = searchText,
                onValueChange = { searchText = it },
                placeholder = { Text("Search payments...", color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)) },
                leadingIcon = { Icon(Icons.Filled.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant) },
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("payments_search_box"),
                shape = RoundedCornerShape(20.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f),
                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f)
                ),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub-filters row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { isFilterExpanded = !isFilterExpanded }
                        .padding(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.FilterList,
                        contentDescription = "Filter",
                        tint = if (isFilterExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "Filter",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isFilterExpanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { showSortDialog = true }
                        .padding(6.dp)
                ) {
                    Text(
                        text = activeSortOption,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Icon(
                        imageVector = Icons.Filled.ArrowDropDown,
                        contentDescription = "Sort By",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            // Expanding Filter Panel
            AnimatedVisibility(
                visible = isFilterExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)
                    ),
                    border = borderStrokeLight()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        // Filter by Session
                        Column {
                            Text(
                                text = "Filter by Session",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("All Sessions", "Morning", "Evening").forEach { ses ->
                                    val active = filterSession == ses
                                    FilterChip(
                                        selected = active,
                                        onClick = { filterSession = ses },
                                        label = { Text(ses, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Filter by Milk Type
                        Column {
                            Text(
                                text = "Filter by Milk Type",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("All Milk Types", "Cow Milk", "Buffalo Milk").forEach { mt ->
                                    val active = filterMilkType == mt
                                    FilterChip(
                                        selected = active,
                                        onClick = { filterMilkType = mt },
                                        label = { Text(mt, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                                        shape = RoundedCornerShape(10.dp),
                                        colors = FilterChipDefaults.filterChipColors(
                                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                                        )
                                    )
                                }
                            }
                        }

                        // Filter by Amount Range
                        Column {
                            Text(
                                text = "Filter by Amount Range",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                OutlinedTextField(
                                    value = filterMinAmount,
                                    onValueChange = { filterMinAmount = it },
                                    placeholder = { Text("Min Amount", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                                Text("-", fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                OutlinedTextField(
                                    value = filterMaxAmount,
                                    onValueChange = { filterMaxAmount = it },
                                    placeholder = { Text("Max Amount", fontSize = 11.sp) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(8.dp),
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedContainerColor = MaterialTheme.colorScheme.surface,
                                        unfocusedContainerColor = MaterialTheme.colorScheme.surface
                                    )
                                )
                            }
                        }

                        // Filter actions
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            OutlinedButton(
                                onClick = {
                                    filterSession = "All Sessions"
                                    filterMilkType = "All Milk Types"
                                    filterMinAmount = ""
                                    filterMaxAmount = ""
                                    selectedSession = "All Sessions"
                                    selectedMilkType = "All Milk Types"
                                    appliedMinAmount = ""
                                    appliedMaxAmount = ""
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Clear Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }

                            Button(
                                onClick = {
                                    selectedSession = filterSession
                                    selectedMilkType = filterMilkType
                                    appliedMinAmount = filterMinAmount
                                    appliedMaxAmount = filterMaxAmount
                                    isFilterExpanded = false
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp)
                            ) {
                                Text("Apply Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (filteredPayments.isEmpty()) {
                // RENDER: Beautiful Empty State
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.ReceiptLong,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f),
                            modifier = Modifier.size(80.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No payments found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = "Start by recording your first payment",
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(20.dp))
                        OutlinedButton(
                            onClick = { currentSubScreen = "ADD_PAYMENT" },
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text("Add Payment", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else {
                // RENDER: Paid List
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    items(filteredPayments) { payment ->
                        PaymentCard(
                            payment = payment,
                            currencySymbol = config.currencySymbol,
                            onClick = { showDeletePymtDialog = payment }
                        )
                    }
                }
            }
        }

        // Floating Action Button
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(bottom = 74.dp, end = 20.dp),
            contentAlignment = Alignment.BottomEnd
        ) {
            FloatingActionButton(
                onClick = { currentSubScreen = "ADD_PAYMENT" },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape,
                modifier = Modifier.size(56.dp)
            ) {
                Icon(Icons.Filled.Add, contentDescription = "Add Payment")
            }
        }
    }



    // Sort dialog selector modal (Image 2 representation)
    if (showSortDialog) {
        AlertDialog(
            onDismissRequest = { showSortDialog = false },
            shape = RoundedCornerShape(20.dp),
            title = {
                Text(
                    text = "Sort By",
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.Start,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    val sortPlans = listOf(
                        "Date (Newest First)",
                        "Date (Oldest First)",
                        "Amount (Highest First)",
                        "Amount (Lowest First)",
                        "Quantity (Highest First)",
                        "Quantity (Lowest First)"
                    )
                    sortPlans.forEach { p ->
                        val active = activeSortOption == p
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    activeSortOption = p
                                    showSortDialog = false
                                }
                                .padding(vertical = 12.dp, horizontal = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = p,
                                fontSize = 14.sp,
                                fontWeight = if (active) FontWeight.Bold else FontWeight.Normal,
                                color = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (active) {
                                Icon(
                                    imageVector = Icons.Filled.Check,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Box(
                    modifier = Modifier.fillMaxWidth(),
                    contentAlignment = Alignment.Center
                ) {
                    TextButton(
                        colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.primary),
                        onClick = { showSortDialog = false }
                    ) {
                        Text("Close", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    }
                }
            }
        )
    }

    // Delete confirmation dialog
    showDeletePymtDialog?.let { payment ->
        AlertDialog(
            onDismissRequest = { showDeletePymtDialog = null },
            title = { Text("Delete Payment Entry?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Are you sure you want to delete the payment of ${config.currencySymbol}${payment.amount} to ${payment.sellerName}?")
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deletePayment(payment.id)
                        showDeletePymtDialog = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = ColorNo),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text("Delete", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeletePymtDialog = null }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// Attractive Payment Card Component
@Composable
fun PaymentCard(
    payment: Payment,
    currencySymbol: String,
    onClick: () -> Unit
) {
    val modeColor = when (payment.paymentMode) {
        "UPI" -> Color(0xFF6200EE)
        "Bank" -> Color(0xFF1976D2)
        "Cash" -> ColorYes
        else -> Color(0xFFE65100)
    }

    val displayDate = remember(payment.date) {
        formatDateDbToDisplay(payment.date)
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(1.dp, RoundedCornerShape(16.dp))
            .clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.08f))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(modeColor.copy(alpha = 0.12f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (payment.paymentMode) {
                        "UPI" -> Icons.Rounded.QrCodeScanner
                        "Bank" -> Icons.Rounded.AccountBalance
                        "Cash" -> Icons.Rounded.Payments
                        else -> Icons.Rounded.ReceiptLong
                    },
                    contentDescription = payment.paymentMode,
                    tint = modeColor,
                    modifier = Modifier.size(20.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = payment.sellerName,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "$currencySymbol${String.format(Locale.US, "%.2f", payment.amount)}",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = modeColor
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = displayDate,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(payment.session, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSecondaryContainer)
                        }

                        Box(
                            modifier = Modifier
                                .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.5f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(payment.milkType, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onTertiaryContainer)
                        }
                    }
                }

                if (payment.notes.isNotBlank()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = payment.notes,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

// Private formats
private fun formatDateDbToDisplay(dbDate: String): String {
    return try {
        val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val formatter = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val date = parser.parse(dbDate) ?: Date()
        formatter.format(date)
    } catch (e: Exception) {
        dbDate
    }
}

private fun formatDateDisplayToDb(displayDate: String): String {
    return try {
        val parser = SimpleDateFormat("dd/MM/yyyy", Locale.US)
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val date = parser.parse(displayDate) ?: Date()
        formatter.format(date)
    } catch (e: Exception) {
        displayDate
    }
}

@Composable
private fun borderStrokeLight(): androidx.compose.foundation.BorderStroke {
    return androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.12f))
}
