package com.example.loginandregistration.security.ui.dashboard

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.loginandregistration.databinding.FragmentSecurityCreateBinding
import com.example.loginandregistration.models.Report
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore

class SecurityCreateFragment : Fragment() {

    private var _binding: FragmentSecurityCreateBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSecurityCreateBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.btnSubmitReport.setOnClickListener {
            submitReport()
        }
    }

    private fun submitReport() {
        val itemName = binding.etItemName.text.toString().trim()
        val description = binding.etDescription.text.toString().trim()
        val location = binding.etLocation.text.toString().trim()
        val currentUser = FirebaseAuth.getInstance().currentUser

        if (itemName.isEmpty() || description.isEmpty() || location.isEmpty()) {
            Toast.makeText(context, "Please fill in all fields", Toast.LENGTH_SHORT).show()
            return
        }

        if (currentUser == null) {
            Toast.makeText(context, "Authentication error. Please log in again.", Toast.LENGTH_SHORT).show()
            return
        }

        setLoading(true)

        // Directly create the report in Firestore without an image
        createReportInFirestore(itemName, description, location, currentUser)
    }

    private fun createReportInFirestore(itemName: String, description: String, location: String, user: com.google.firebase.auth.FirebaseUser) {
        val db = FirebaseFirestore.getInstance()

        // This assumes your Report.kt data class has a constructor parameter named 'name'.
        // If it's 'itemName', change 'name = itemName' to 'itemName = itemName'.
        val report = Report(
            itemName = itemName, // <-- FIX: Changed 'name' to 'itemName' to match Report.kt
            description = description,
            location = location,
            imageUrl = null, // No image URL
            status = "Pending",
            category = "Found",

            userEmail = user.email ?: "",
            userRole = "Security"
        )

        db.collection("items")
            .add(report)
            .addOnSuccessListener {
                Toast.makeText(context, "Found item report submitted successfully!", Toast.LENGTH_SHORT).show()
                setLoading(false)
                // Navigate back to the previous screen (e.g., the reports list)
                activity?.supportFragmentManager?.popBackStack()
            }
            .addOnFailureListener { e ->
                Toast.makeText(context, "Failed to submit report: ${e.message}", Toast.LENGTH_LONG).show()
                setLoading(false)
            }
    }

    private fun setLoading(isLoading: Boolean) {
        binding.btnSubmitReport.isEnabled = !isLoading
        binding.btnSubmitReport.text = if (isLoading) "Submitting..." else "Submit Found Item Report"
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}

