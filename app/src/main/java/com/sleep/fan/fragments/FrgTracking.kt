package com.sleep.fan.fragments

import android.app.DatePickerDialog
import android.app.Dialog
import android.app.TimePickerDialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.databinding.DataBindingUtil
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import androidx.lifecycle.lifecycleScope
import com.google.android.material.tabs.TabLayout
import com.sleep.fan.R
import com.sleep.fan.databinding.FragmentTrackingBinding
import com.sleep.fan.fragments.trackFragments.DayFragment
import com.sleep.fan.fragments.trackFragments.MonthFragment
import com.sleep.fan.fragments.trackFragments.WeekFragment
import com.sleep.fan.newdb.NewSleepDatabase
import com.sleep.fan.newdb.SleepDao
import com.sleep.fan.newdb.SleepDataGenerator
import com.sleep.fan.utility.DatePickerFragment
import com.sleep.fan.utility.TimePickerFragment
import kotlinx.coroutines.launch
import java.text.ParseException
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale


class FrgTracking : Fragment(), DatePickerDialog.OnDateSetListener,
    TimePickerDialog.OnTimeSetListener {
    private lateinit var binding: FragmentTrackingBinding
    private val TAG = "MainActivity"

    private var isSleepTime = true


    private var selectedYear: Int = 0
    private var selectedMonth: Int = 0
    private var selectedDay: Int = 0
    private var selectedHour: Int = 0
    private var selectedMinute: Int = 0

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = DataBindingUtil.inflate(inflater, R.layout.fragment_tracking, container, false)
        val view = binding.root

        // Setup TabLayout with listeners
        setupTabLayout()

        // Initially display the first fragment
        replaceFragment(DayFragment())

        binding.imgAddData.setOnClickListener {
            showDialog()
        }

        return view
    }

    private fun setupTabLayout() {
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("D")) // Day
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("W")) // Week
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("M")) // Month

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                when (tab.position) {
                    0 -> replaceFragment(DayFragment())
                    1 -> replaceFragment(WeekFragment())
                    2 -> replaceFragment(MonthFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {
                // Do nothing
            }

            override fun onTabReselected(tab: TabLayout.Tab) {
                // Do nothing
            }
        })
    }

    private fun replaceFragment(fragment: Fragment) {
        val transaction: FragmentTransaction = childFragmentManager.beginTransaction()
        transaction.replace(R.id.fragment_container, fragment)
        transaction.commit()
    }

    @Throws(ParseException::class)
    private fun insertSampleData(context: Context, sleepTimeStr: String, awakeTimeStr: String) {
        val sleepDao: SleepDao = NewSleepDatabase.getInstance(context).sleepDao()

//        lifecycleScope.launch {
//            SleepDataGenerator.generateAndInsertSleepData(
//                requireContext(),
//                sleepTimeStr,
//                awakeTimeStr
//            );
//        }
    }

    var dialog: Dialog? = null

    fun showDialog() {
        dialog = context?.let { Dialog(it) }
        dialog!!.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog!!.setCancelable(false)
        dialog!!.setContentView(R.layout.dialog_add_sleep_data)
        val tvSleepTime = dialog!!.findViewById<TextView>(R.id.tvSleepTime)
        val tvAwakeTime = dialog!!.findViewById<TextView>(R.id.tvAwakeTime)
        val btn_dialog = dialog!!.findViewById<TextView>(R.id.btn_dialog)
        val ivExit = dialog!!.findViewById<ImageView>(R.id.ivExit)

        tvSleepTime.setOnClickListener {
            isSleepTime = true
            val datePickerFragment = DatePickerFragment(this)
            datePickerFragment.show(childFragmentManager, "DATE_PICK")
        }

        tvAwakeTime.setOnClickListener {
            isSleepTime = false
            val datePickerFragment = DatePickerFragment(this)
            datePickerFragment.show(childFragmentManager, "DATE_PICK")
        }

        btn_dialog.setOnClickListener {
            if(!TextUtils.isEmpty(tvSleepTime.text) && !TextUtils.isEmpty(tvAwakeTime.text)){
                try {
                    context?.let {
                        insertSampleData(it, tvSleepTime.text.toString(), tvAwakeTime.text.toString())
                    }
                } catch (e: ParseException) {
                    e.printStackTrace()
                }
            }else{
                Toast.makeText(context, "Add Sleep and Awake time", Toast.LENGTH_SHORT).show()
            }
            dialog!!.dismiss()
        }

        ivExit.setOnClickListener { dialog!!.dismiss() }
        dialog!!.show()
    }

    override fun onDateSet(
        view: android.widget.DatePicker?,
        year: Int,
        month: Int,
        dayOfMonth: Int,
    ) {
        selectedYear = year
        selectedMonth = month
        selectedDay = dayOfMonth

        // Show the time picker dialog after date is set
        val timePickerFragment = TimePickerFragment(this)
        timePickerFragment.show(childFragmentManager, "TIME_PICK")
    }

    override fun onTimeSet(view: android.widget.TimePicker?, hourOfDay: Int, minute: Int) {
        selectedHour = hourOfDay
        selectedMinute = minute

        val calendar = Calendar.getInstance()
        calendar.set(selectedYear, selectedMonth, selectedDay, selectedHour, selectedMinute)

        val dateFormat = SimpleDateFormat("dd-MMMM-yyyy hh:mm a", Locale.getDefault())
        val formattedDate = dateFormat.format(calendar.time)

        val dialog = dialog
        val tvSleepTime = dialog?.findViewById<TextView>(R.id.tvSleepTime)
        val tvAwakeTime = dialog?.findViewById<TextView>(R.id.tvAwakeTime)

        if (isSleepTime) {
            tvSleepTime?.text = formattedDate
        } else {
            tvAwakeTime?.text = formattedDate
        }
    }
}
