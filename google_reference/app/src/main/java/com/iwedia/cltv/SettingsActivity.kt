package com.iwedia.cltv

import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.iwedia.cltv.config.ConfigStringsManager
import com.iwedia.cltv.fti.*
import com.iwedia.cltv.fti.cable.*
import com.iwedia.cltv.fti.data.Channel
import com.iwedia.cltv.fti.handlers.ChannelHandler
import com.iwedia.cltv.fti.scan_models.DataParams
import com.iwedia.cltv.fti.terrestrial.ScanProgressFtiFragment
import com.iwedia.cltv.fti.terrestrial.ScanTuneOptionFragment
import com.iwedia.cltv.fti.terrestrial.TerManualScanParamsFragment
import java.util.*


class SettingsActivity : AppCompatActivity() {

    enum class FtiFragm {
        parentFragm, tunerFragm, countryTerFragm, optionTerFragm, manualTerFragm, progressTerFragm, scanDoneFragm, lcnConfFragm, lcnChooseFragm, providerCabFragm, optionCabFragm, manualCabFragm, progressCabFragm    }
    var fragm : FtiFragm? = null

    var fragmentManager = this.supportFragmentManager
    var fragmentTransaction: FragmentTransaction = fragmentManager!!.beginTransaction()

    val parentalPinFtiFragment: ParentalPinFtiFragment = ParentalPinFtiFragment.newInstance()
    val tunerFtiFragment: TunerFtiFragment = TunerFtiFragment.newInstance()
    val countrySelectFtiFragment: CountrySelectFtiFragment = CountrySelectFtiFragment.newInstance()
    val scanTuneOptionFragment: ScanTuneOptionFragment = ScanTuneOptionFragment.newInstance()
    val terManualScanParamsFragment: TerManualScanParamsFragment = TerManualScanParamsFragment.newInstance()
    val scanProgressFtiFragment: ScanProgressFtiFragment = ScanProgressFtiFragment.newInstance()
    val scanDoneFtiFragment: ScanDoneFtiFragment = ScanDoneFtiFragment.newInstance()
    val lcnConflictFragment: LcnConflictFragment = LcnConflictFragment.newInstance()
    val chooseLcnFragment: ChooseLcnFragment = ChooseLcnFragment.newInstance()

    val cableSelectProviderFtiFragment: CableSelectProviderFtiFragment = CableSelectProviderFtiFragment.newInstance()
    val scanTuneCableOptionFragment: ScanTuneCableOptionFragment = ScanTuneCableOptionFragment.newInstance()
    val cableManualScanParamsFragment: CableManualScanParamsFragment = CableManualScanParamsFragment.newInstance()
    val cableScanProgressFtiFragment: CableScanProgressFtiFragment = CableScanProgressFtiFragment.newInstance()

    var dataParams: DataParams? = null

    var autoManual = 0
    var channelNum = 0
    var channelFreq = 0
    var channelBand = 0

    var lcnConflictNumber = 0
    var programmesFound = ""
    var mChannelsHandler: ChannelHandler? = null
    var mChannels: List<Channel>? = null
    var hasLcnConflicts = false
    var atLcnConflictNumber = 0
    var atChannels: MutableList<Channel>? = null
    var conflictFixedList: MutableList<Boolean> = mutableListOf<Boolean>()
    var channelKey = 0

    var manualQuick = -1
    var cableProviderName = ""

    var countryName = ""
    var buttonNetworkId = 0
    var buttonFreq = 0
    var buttonSymbolRate = 0
    var numberPickerChannelQam = 0
    var numberPickerChannelStandard = 0

    var terrestrialCable = -1

    val testFtiFragm: TestFragment = TestFragment.newInstance()

    var atvGtvSwitch = 0

    /** Module provider */
    lateinit var moduleProvider: ModuleProvider

    @RequiresApi(Build.VERSION_CODES.R)
    override fun onCreate(savedInstanceState: Bundle?) {
        moduleProvider = ModuleProvider(this.application)
        super.onCreate(savedInstanceState)
        supportActionBar!!.hide()
        moduleProvider.getGeneralConfigModule().setup(applicationContext.resources.openRawResource(R.raw.general_settings))

        setContentView(R.layout.fti_settings_activity)
        ConfigStringsManager.setup(moduleProvider.getUtilsModule().getCountry())

        if (savedInstanceState == null) {
            if (!isFinishing()) {
//                fragmentTransaction.replace(R.id.relativeLayout, testFtiFragm).commitNow()
                fragmentTransaction.replace(R.id.relativeLayout, parentalPinFtiFragment).commitNow()
//                fragmentTransaction.replace(R.id.relativeLayout, countrySelectFtiFragment).commitNow()
//                fragmentTransaction.replace(R.id.relativeLayout, tunerFtiFragment).commitNow()
                fragm = FtiFragm.parentFragm
//                fragm = FtiFragm.countryTerFragm
            }
        }
    }

    fun goToPinFragment(){
        fragmentManager = this.supportFragmentManager
        fragmentTransaction= fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, parentalPinFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.parentFragm
    }

    fun goToTunerFragment(tdp: DataParams){
        this.dataParams = tdp
        fragmentManager = this.supportFragmentManager
        fragmentTransaction= fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, tunerFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.tunerFragm
    }

    fun goToTunerFragment(){
        fragmentManager = this.supportFragmentManager
        fragmentTransaction= fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, tunerFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.tunerFragm
    }

    fun goToCountrySelectFragment(){
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, countrySelectFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.countryTerFragm
    }

    fun goToTuneOptionFragment() {
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, scanTuneOptionFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.optionTerFragm
    }

    fun goToTerManualScan() {
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, terManualScanParamsFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.manualTerFragm
    }

    fun goToScanProgressScan() {
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, scanProgressFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.progressTerFragm
    }

    fun goToScanDone() {
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, scanDoneFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.scanDoneFragm
    }

    fun goToLcnConflict() {
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, lcnConflictFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.lcnConfFragm
    }

    fun goToChooseLcn(i: Int, get: MutableList<Channel>?, key: Int) {
        atLcnConflictNumber = i
        atChannels = get
        channelKey = key
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, chooseLcnFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.lcnChooseFragm
    }

    fun goToCableSelectProviderFragment(){
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, cableSelectProviderFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.providerCabFragm
    }

    fun goToScanTuneCableOptionFragment(){
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, scanTuneCableOptionFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.optionCabFragm
    }

    fun goToCableManualScanFragment(){
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, cableManualScanParamsFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.manualCabFragm
    }

    fun goToCableScanProgressFragment(){
        fragmentManager = this.supportFragmentManager
        fragmentTransaction = fragmentManager!!.beginTransaction()
        fragmentTransaction.replace(R.id.relativeLayout, cableScanProgressFtiFragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.commit()
        fragm = FtiFragm.progressCabFragm
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        if(fragm == FtiFragm.parentFragm) {
            parentalPinFtiFragment.myOnKeyDown(keyCode)
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true
            }
        }
        else if(fragm == FtiFragm.countryTerFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
//                goToCountrySelectFragment()
                return true
            }
        }
        else if(fragm == FtiFragm.tunerFragm) {
            tunerFtiFragment.myOnKeyDown(keyCode)
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goToCountrySelectFragment()
                return true
            }
        }
        else if(fragm == FtiFragm.optionTerFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goToTunerFragment()
                return true
            }
        }
        else if(fragm == FtiFragm.manualTerFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goToTuneOptionFragment()
                return true
            }
        }else if(fragm == FtiFragm.progressTerFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if(autoManual == 0){
                    goToTuneOptionFragment()
                }else {
                    goToTerManualScan()
                }
                return true
            }
        }else if(fragm ==  FtiFragm.scanDoneFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if (terrestrialCable == 0) {
                    goToTuneOptionFragment()
                }else{
                    goToScanTuneCableOptionFragment()
                }
                return true
            }
        }else if(fragm == FtiFragm.lcnConfFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true
            }
        }else if(fragm == FtiFragm.lcnChooseFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                return true
            }
        }
        else if(fragm == FtiFragm.providerCabFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goToTunerFragment()
                return true
            }
        }
        else if(fragm == FtiFragm.optionCabFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                if(cableProviderName.equals("none")){
                    goToTunerFragment()
                }else{
                    goToCableSelectProviderFragment()
                }
                return true
            }
        }
        else if(fragm == FtiFragm.manualCabFragm) {
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goToScanTuneCableOptionFragment()
                return true
            }else if(keyCode == KeyEvent.KEYCODE_DPAD_RIGHT){
                cableManualScanParamsFragment.myOnKeyDown(keyCode)
                return true
            }else if(keyCode == KeyEvent.KEYCODE_DPAD_LEFT){
                cableManualScanParamsFragment.myOnKeyDown(keyCode)
                return true
            }
        }else if(fragm == FtiFragm.progressCabFragm){
            if (keyCode == KeyEvent.KEYCODE_BACK) {
                goToCableManualScanFragment()
                return true
            }
        }
        return super.onKeyDown(keyCode, event)
    }

}