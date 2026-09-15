package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entity.InventoryItemEntity
import com.example.ui.MainViewModel
import com.example.ui.theme.MikroTikPrimary

@Composable
fun InventoryScreen(viewModel: MainViewModel, onNavigateToStudio: () -> Unit, onNavigateToBatches: () -> Unit) {
    val items by viewModel.inventoryItems.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        "مخزن الكروت (الرصيد الفعلي)",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("إدارة كروت المستودع الجاهزة", fontSize = 12.sp, color = Color.Gray)
                }
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Button(
                        onClick = onNavigateToStudio,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("استوديو الكروت", color = MikroTikPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                    Button(
                        onClick = onNavigateToBatches,
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text("سجل الدفعات", color = MikroTikPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            if (items.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("المخزن فارغ حالياً. أضف كروت للبدء.", color = Color.Gray)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    items(items) { item ->
                        InventoryItemCard(item = item)
                    }
                }
            }
        }

        FloatingActionButton(
            onClick = { showAddDialog = true },
            containerColor = MikroTikPrimary,
            contentColor = Color.White,
            modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "إضافة للمخزن")
        }
    }

    if (showAddDialog) {
        AddInventoryDialog(
            onDismiss = { showAddDialog = false },
            onConfirm = { packageName, qty, wholesale, retail ->
                viewModel.saveInventoryItem(packageName, qty, wholesale, retail)
                showAddDialog = false
            }
        )
    }
}

@Composable
fun InventoryItemCard(item: InventoryItemEntity) {
    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(16.dp).fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(text = item.packageName, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("سعر الجملة: ${item.wholesalePrice.toInt()} ريال", fontSize = 12.sp, color = Color.Gray)
                Text("سعر الجمهور: ${item.retailPrice.toInt()} ريال", fontSize = 12.sp, color = Color.Gray)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primary),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("الرصيد", fontSize = 10.sp, color = Color.White)
                    Text("${item.quantityAvailable}", fontWeight = FontWeight.Bold, fontSize = 18.sp, color = Color.White)
                }
            }
        }
    }
}

@Composable
fun AddInventoryDialog(
    onDismiss: () -> Unit,
    onConfirm: (packageName: String, qty: Int, wholesale: Double, retail: Double) -> Unit
) {
    var packageName by remember { mutableStateOf("") }
    var quantity by remember { mutableStateOf("") }
    var wholesalePrice by remember { mutableStateOf("") }
    var retailPrice by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("إضافة كروت للمخزن", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = packageName,
                    onValueChange = { packageName = it },
                    label = { Text("الباقة (مثال: فئة 200 ريال)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = quantity,
                    onValueChange = { quantity = it },
                    label = { Text("عدد الكروت المضافة") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = wholesalePrice,
                    onValueChange = { wholesalePrice = it },
                    label = { Text("سعر الجملة (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = retailPrice,
                    onValueChange = { retailPrice = it },
                    label = { Text("سعر البيع للجمهور (ريال)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val qty = quantity.toIntOrNull() ?: 0
                    val wPrice = wholesalePrice.toDoubleOrNull() ?: 0.0
                    val rPrice = retailPrice.toDoubleOrNull() ?: 0.0
                    if (packageName.isNotBlank() && qty > 0) {
                        onConfirm(packageName, qty, wPrice, rPrice)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = MikroTikPrimary)
            ) {
                Text("إضافة")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("إلغاء")
            }
        }
    )
}
