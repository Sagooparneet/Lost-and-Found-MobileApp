package com.example.loginandregistration

import android.Manifest
import android.app.DatePickerDialog
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.Timestamp
import com.example.loginandregistration.admin.utils.StoragePermissionHelper
import com.example.loginandregistration.utils.EditTextUtils
import com.example.loginandregistration.utils.NotificationManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.UUID

class ReportFragment : Fragment() {

    private lateinit var etItemName: EditText
    private lateinit var etDescription: EditText
    private lateinit var etLocation: EditText
    private lateinit var etContactInfo: EditText
    private lateinit var etDate: EditText
    private lateinit var btnPickDate: Button
    private lateinit var btnUploadImage: Button
    private lateinit var ivPreview: ImageView
    private lateinit var rgItemType: RadioGroup
    private lateinit var spinnerCategory: Spinner
    private lateinit var btnSubmit: Button
    private lateinit var progressBar: android.widget.ProgressBar
    private lateinit var scrollView: android.widget.ScrollView

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private val storage = FirebaseStorage.getInstance()

    private var isForSecurity: Boolean = false
    private var preSelectedType: String? = null
    private var selectedCategory: String = ""
    private var selectedDate: Date? = null
    private var selectedImageUri: Uri? = null
    private var uploadedImageUrl: String? = null
    private val dateFormat = SimpleDateFormat("MMM dd, yyyy", Locale.getDefault())
    
    // Image picker launcher
    private val imagePickerLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == android.app.Activity.RESULT_OK) {
            result.data?.data?.let { uri ->
                selectedImageUri = uri
                displayImagePreview(uri)
            }
        }
    }
    
    // Permission launcher for storage access
    // Requirements: 4.2, 4.3, 4.4, 4.5
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            // Permission granted, proceed with image picker
            launchImagePicker()
        } else {
            // Permission denied
            Toast.makeText(
                context,
                "Storage permission is required to select images",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    companion object {
        private const val TAG = "ReportFragment"
        private const val ARG_IS_SECURITY = "is_security_creating_report"
        const val ARG_REPORT_TYPE = "report_type"
        const val TYPE_LOST = "lost"
        const val TYPE_FOUND = "found"

        /**
         * Use this factory method to create a new instance of
         * this fragment.
         *
         * @param isSecurityCreatingReport True if the security staff is creating a report.
         * @return A new instance of fragment ReportFragment.
         */
        @JvmStatic
        fun newInstance(isSecurityCreatingReport: Boolean = false): ReportFragment {
            val fragment = ReportFragment()
            val args = Bundle()
            args.putBoolean(ARG_IS_SECURITY, isSecurityCreatingReport)
            fragment.arguments = args
            return fragment
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Retrieve the arguments
        arguments?.let {
            isForSecurity = it.getBoolean(ARG_IS_SECURITY)
            preSelectedType = it.getString(ARG_REPORT_TYPE)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_report, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        etItemName = view.findViewById(R.id.et_item_name)
        etDescription = view.findViewById(R.id.et_description)
        etLocation = view.findViewById(R.id.et_location)
        etContactInfo = view.findViewById(R.id.et_contact_info)
        etDate = view.findViewById(R.id.et_date)
        btnPickDate = view.findViewById(R.id.btn_pick_date)
        btnUploadImage = view.findViewById(R.id.btn_upload_image)
        ivPreview = view.findViewById(R.id.iv_preview)
        rgItemType = view.findViewById(R.id.rg_item_type)
        spinnerCategory = view.findViewById(R.id.spinner_category)
        btnSubmit = view.findViewById(R.id.btn_submit)
        progressBar = view.findViewById(R.id.progress_bar)
        scrollView = view.findViewById(R.id.scroll_view)

        // Set up category spinner
        setupCategorySpinner()
        
        // Set up date picker
        setupDatePicker()
        
        // Set up image upload
        setupImageUpload()

        // Handle pre-selected report type
        handlePreSelectedType()
        
        // You can now use the 'isForSecurity' flag to change the UI or logic
        if (isForSecurity) {
            // Example: Change the submit button text for security
            btnSubmit.text = "Create Found Item Report"
        }

        btnSubmit.setOnClickListener {
            submitReport()
        }
    }

    /**
     * Handle pre-selected report type from navigation
     * Requirements: 3.1, 3.2, 3.4
     */
    private fun handlePreSelectedType() {
        preSelectedType?.let { type ->
            when (type) {
                TYPE_LOST -> {
                    rgItemType.check(R.id.rb_lost)
                    // Hide radio group when type is pre-selected
                    rgItemType.visibility = View.GONE
                    // Also hide the label
                    view?.findViewById<TextView>(R.id.tv_item_status_label)?.visibility = View.GONE
                }
                TYPE_FOUND -> {
                    rgItemType.check(R.id.rb_found)
                    // Hide radio group when type is pre-selected
                    rgItemType.visibility = View.GONE
                    // Also hide the label
                    view?.findViewById<TextView>(R.id.tv_item_status_label)?.visibility = View.GONE
                }
            }
        }
    }
    
    private fun setupCategorySpinner() {
        val categories = resources.getStringArray(R.array.item_categories)
        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            categories
        )
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerCategory.adapter = adapter
        
        // Set listener to capture selected category
        spinnerCategory.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: android.widget.AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedCategory = if (position == 0) "" else categories[position]
            }
            
            override fun onNothingSelected(parent: android.widget.AdapterView<*>?) {
                selectedCategory = ""
            }
        }
    }
    
    private fun setupDatePicker() {
        val calendar = Calendar.getInstance()
        
        val datePickerListener = DatePickerDialog.OnDateSetListener { _, year, month, dayOfMonth ->
            calendar.set(Calendar.YEAR, year)
            calendar.set(Calendar.MONTH, month)
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth)
            
            selectedDate = calendar.time
            etDate.setText(dateFormat.format(selectedDate!!))
        }
        
        // Set click listeners for both the EditText and button
        val showDatePicker = {
            DatePickerDialog(
                requireContext(),
                datePickerListener,
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
            ).show()
        }
        
        etDate.setOnClickListener { showDatePicker() }
        btnPickDate.setOnClickListener { showDatePicker() }
    }
    
    private fun setupImageUpload() {
        btnUploadImage.setOnClickListener {
            openImagePicker()
        }
    }
    
    /**
     * Opens image picker with version-aware permission handling
     * Requirements: 4.2, 4.3, 4.4, 4.5
     */
    private fun openImagePicker() {
        // Check if permissions are needed and granted
        if (hasStoragePermission()) {
            launchImagePicker()
        } else {
            // Request permissions based on Android version
            requestStoragePermission()
        }
    }
    
    /**
     * Checks if storage permissions are granted based on Android version
     * Requirements: 4.2, 4.3, 4.4
     */
    private fun hasStoragePermission(): Boolean {
        val permissions = StoragePermissionHelper.getStoragePermissions()
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(requireContext(), permission) == 
                PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Requests storage permissions based on Android version
     * Requirements: 4.2, 4.3, 4.4, 4.5
     */
    private fun requestStoragePermission() {
        val permissions = StoragePermissionHelper.getStoragePermissions()
        permissionLauncher.launch(permissions)
    }
    
    /**
     * Launches the image picker intent
     */
    private fun launchImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        intent.type = "image/*"
        imagePickerLauncher.launch(intent)
    }
    
    private fun displayImagePreview(uri: Uri) {
        try {
            ivPreview.setImageURI(uri)
            ivPreview.visibility = View.VISIBLE
        } catch (e: Exception) {
            Log.e(TAG, "Error displaying image preview", e)
            Toast.makeText(context, "Error loading image", Toast.LENGTH_SHORT).show()
        }
    }
    
    private suspend fun uploadImageToStorage(itemId: String): String? {
        return withContext(Dispatchers.IO) {
            try {
                selectedImageUri?.let { uri ->
                    // Compress the image
                    val compressedData = compressImage(uri)
                    
                    // Create storage reference
                    val storageRef = storage.reference
                    val imageRef = storageRef.child("items/$itemId/${UUID.randomUUID()}.jpg")
                    
                    // Upload the compressed image
                    val uploadTask = imageRef.putBytes(compressedData).await()
                    
                    // Get download URL
                    val downloadUrl = imageRef.downloadUrl.await()
                    downloadUrl.toString()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error uploading image", e)
                withContext(Dispatchers.Main) {
                    Toast.makeText(context, getString(R.string.image_upload_failed), Toast.LENGTH_SHORT).show()
                }
                null
            }
        }
    }
    
    private fun compressImage(uri: Uri): ByteArray {
        val inputStream = requireContext().contentResolver.openInputStream(uri)
        val bitmap = BitmapFactory.decodeStream(inputStream)
        inputStream?.close()
        
        // Calculate scaling to keep image under 1MB
        var quality = 90
        val outputStream = ByteArrayOutputStream()
        
        // Resize if image is too large
        val maxDimension = 1920
        val scaledBitmap = if (bitmap.width > maxDimension || bitmap.height > maxDimension) {
            val scale = maxDimension.toFloat() / Math.max(bitmap.width, bitmap.height)
            val newWidth = (bitmap.width * scale).toInt()
            val newHeight = (bitmap.height * scale).toInt()
            Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true)
        } else {
            bitmap
        }
        
        // Compress to JPEG
        do {
            outputStream.reset()
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, quality, outputStream)
            quality -= 10
        } while (outputStream.size() > 1024 * 1024 && quality > 10) // Keep under 1MB
        
        if (scaledBitmap != bitmap) {
            scaledBitmap.recycle()
        }
        bitmap.recycle()
        
        return outputStream.toByteArray()
    }

    private fun submitReport() {
        val itemName = etItemName.text.toString().trim()
        val description = etDescription.text.toString().trim()
        val location = etLocation.text.toString().trim()
        val contactInfo = etContactInfo.text.toString().trim()

        // Validate required fields
        if (itemName.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Contact info might not be needed if security is filing the report
        if (!isForSecurity && contactInfo.isEmpty()) {
            Toast.makeText(context, getString(R.string.error_empty_fields), Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate category selection
        if (selectedCategory.isEmpty()) {
            Toast.makeText(context, "Please select a category", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Validate date selection
        if (selectedDate == null) {
            Toast.makeText(context, "Please select a date", Toast.LENGTH_SHORT).show()
            return
        }

        val isLost = rgItemType.checkedRadioButtonId == R.id.rb_lost
        val currentUser = auth.currentUser

        if (currentUser == null) {
            Toast.makeText(context, "Please login first", Toast.LENGTH_SHORT).show()
            return
        }

        // Use lifecycleScope to launch coroutine on IO dispatcher
        viewLifecycleOwner.lifecycleScope.launch(Dispatchers.IO) {
            try {
                // Show loading indicator on Main thread
                withContext(Dispatchers.Main) {
                    showLoading()
                }
                
                // First, create the item document to get an ID
                val itemRef = db.collection("items").document()
                val itemId = itemRef.id
                
                // Upload image if selected
                var imageUrl: String? = null
                if (selectedImageUri != null) {
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, getString(R.string.uploading_image), Toast.LENGTH_SHORT).show()
                    }
                    imageUrl = uploadImageToStorage(itemId)
                }
                
                // Create the item with all fields
                val item = LostFoundItem(
                    id = itemId,
                    name = itemName,
                    description = description,
                    location = location,
                    contactInfo = if (isForSecurity) "Held by Security" else contactInfo,
                    category = selectedCategory,
                    dateLostFound = Timestamp(selectedDate!!),
                    imageUrl = imageUrl,
                    isLost = isLost,
                    status = if (isLost) "Approved" else "Pending Approval",
                    userId = currentUser.uid,
                    userEmail = currentUser.email ?: "",
                    timestamp = Timestamp.now()
                )
                
                // Save to Firestore
                itemRef.set(item).await()
                
                // Send notification if it's a found item (requires approval)
                // Requirements: 11.1
                if (!isLost) {
                    NotificationManager.notifyFoundItemSubmitted(itemId, itemName)
                }
                
                // Show success message on Main thread
                withContext(Dispatchers.Main) {
                    hideLoading()
                    Toast.makeText(context, getString(R.string.item_reported_successfully), Toast.LENGTH_SHORT).show()
                    clearForm()
                }
            } catch (e: FirebaseFirestoreException) {
                // Handle Firestore-specific errors
                handleFirestoreError(e)
            } catch (e: Exception) {
                // Handle generic errors
                handleGenericError(e)
            }
        }
    }
    
    private suspend fun handleFirestoreError(e: FirebaseFirestoreException) {
        Log.e(TAG, "Firestore error: ${e.code} - ${e.message}", e)
        
        withContext(Dispatchers.Main) {
            hideLoading()
            
            val message = when (e.code) {
                FirebaseFirestoreException.Code.PERMISSION_DENIED -> 
                    "Access denied. Please check your permissions and try again."
                FirebaseFirestoreException.Code.UNAVAILABLE -> 
                    "Network error. Please check your connection and try again."
                FirebaseFirestoreException.Code.UNAUTHENTICATED -> 
                    "Authentication required. Please sign in again."
                else -> 
                    "Error submitting report: ${e.message}"
            }
            
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
    
    private suspend fun handleGenericError(e: Exception) {
        Log.e(TAG, "Error submitting report: ${e.message}", e)
        
        withContext(Dispatchers.Main) {
            hideLoading()
            Toast.makeText(
                context,
                "Failed to submit report. Please try again.",
                Toast.LENGTH_LONG
            ).show()
        }
    }

    private fun clearForm() {
        EditTextUtils.safeClear(etItemName)
        EditTextUtils.safeClear(etDescription)
        EditTextUtils.safeClear(etLocation)
        EditTextUtils.safeClear(etContactInfo)
        EditTextUtils.safeClear(etDate)
        rgItemType.clearCheck()
        spinnerCategory.setSelection(0)
        selectedCategory = ""
        selectedDate = null
        selectedImageUri = null
        uploadedImageUrl = null
        ivPreview.setImageURI(null)
        ivPreview.visibility = View.GONE
    }
    
    private fun showLoading() {
        progressBar.visibility = View.VISIBLE
        scrollView.visibility = View.GONE
        btnSubmit.isEnabled = false
    }
    
    private fun hideLoading() {
        progressBar.visibility = View.GONE
        scrollView.visibility = View.VISIBLE
        btnSubmit.isEnabled = true
    }
}
