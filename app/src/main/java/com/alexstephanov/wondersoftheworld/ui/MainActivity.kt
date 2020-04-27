package com.alexstephanov.wondersoftheworld.ui

import android.content.Context
import android.net.ConnectivityManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import com.alexstephanov.wondersoftheworld.R
import com.alexstephanov.wondersoftheworld.database.WondersDao
import com.alexstephanov.wondersoftheworld.database.WondersDatabase
import com.alexstephanov.wondersoftheworld.model.*
import com.alexstephanov.wondersoftheworld.server_api.NetworkService
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class MainActivity<T> : AppCompatActivity(), MainFragment.OnFragmentEventListener<T>, DetailedFragment.OnFragmentEventListener, NatureWondersDetailedFragment.OnFragmentEventListener {

    private var ancientFragment: MainFragment<AncientWondersListItemModel>? = null
    private var modernFragment: MainFragment<ModernWondersListItemModel>? = null
    private var natureFragment: MainFragment<NatureWondersListItemModel>? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setSupportActionBar(toolbar_main)

        val wondersDao = WondersDatabase.getInstance(this).getWondersDao()

        if(checkNetworkConnection(this)) {
            GlobalScope.launch {
                val dataModelResult = getDataFromNetwork()
                showDatabaseStatus(wondersDao)
                clearAllTablesInDatabase(wondersDao)
                showDatabaseStatus(wondersDao)
                addAllItemsToDatabase(wondersDao, dataModelResult.listModel.ancientWonders, dataModelResult.listModel.modernWonders, dataModelResult.listModel.natureWonders)
                prepareData(dataModelResult.listModel.ancientWonders, dataModelResult.listModel.modernWonders, dataModelResult.listModel.natureWonders)
                supportFragmentManager.beginTransaction().replace(R.id.fragment_place, ancientFragment!!).commitAllowingStateLoss()
                Log.d(TAG, "Load data from network.")
            }

        } else {
            GlobalScope.launch(Dispatchers.IO) {
                val listModelResult = getDataFromDatabase(wondersDao)
                if(listModelResult.ancientWonders.isNotEmpty() and
                        listModelResult.modernWonders.isNotEmpty() and
                        listModelResult.natureWonders.isNotEmpty()) {
                    prepareData(listModelResult.ancientWonders, listModelResult.modernWonders, listModelResult.natureWonders)
                    supportFragmentManager.beginTransaction().replace(R.id.fragment_place, ancientFragment!!).commitAllowingStateLoss()
                    showNetworkLostConnectionTextView(OFFLINE_MODE)
                    Log.d(TAG, "Load data from database.")
                } else {
                    showNetworkLostConnectionTextView(LOST_CONNECTION)
                    Log.d(TAG, "Can't load data.")
                }
            }
        }
    }

    private fun checkNetworkConnection(context: Context):Boolean {
        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = connectivityManager.activeNetworkInfo
        return  networkInfo != null && networkInfo.isConnected
    }

    private fun showNetworkLostConnectionTextView(status: Int) {
        when(status) {
            OFFLINE_MODE -> {
                toolbar_button_network_main.setImageResource(R.drawable.ic_warning)
                toolbar_button_network_main.setOnClickListener {
                    Toast.makeText(this, "Соединение с сервером отсутствует.\nВключен оффлайн режим.", Toast.LENGTH_LONG).show()
                }
            }
            LOST_CONNECTION -> {
                toolbar_button_network_main.setImageResource(R.drawable.ic_error)
                toolbar_button_network_main.setOnClickListener {
                    Toast.makeText(this, "Соединение с сервером отсутствует.\nПожалуйста проверьте подключение к интернету!", Toast.LENGTH_LONG).show()
                }
            }
        }
        toolbar_button_network_main.background = null
        toolbar_button_network_main.visibility = View.VISIBLE
    }

    private suspend fun addAllItemsToDatabase(wondersDao: WondersDao, ancientWondersList: List<AncientWondersListItemModel>, modernWondersList: List<ModernWondersListItemModel>, natureWondersList: List<NatureWondersListItemModel>) {
        wondersDao.addAncientsWondersItems(ancientWondersList)
        wondersDao.addModernWondersItems(modernWondersList)
        wondersDao.addNatureWondersItems(natureWondersList)
    }

    private suspend fun clearAllTablesInDatabase(wondersDao: WondersDao) {
        wondersDao.clearAncientWondersTable()
        wondersDao.clearModernWondersTable()
        wondersDao.clearNatureWondersTable()
    }

    private fun showDatabaseStatus(wondersDao: WondersDao) {
        Log.d(TAG, wondersDao.getAllAncientWondersItems().toString())
        Log.d(TAG, wondersDao.getAllModernWondersItems().toString())
        Log.d(TAG, wondersDao.getAllNatureWondersItems().toString())
    }

    private fun prepareData(ancientWondersList: List<AncientWondersListItemModel>, modernWondersList: List<ModernWondersListItemModel>, natureWondersList: List<NatureWondersListItemModel>) {
        ancientFragment = MainFragment(ancientWondersList, LAYOUT_ANCIENT)
        modernFragment = MainFragment(modernWondersList, LAYOUT_MODERN)
        natureFragment = MainFragment(natureWondersList, LAYOUT_NATURE)
    }

    private suspend fun getDataFromDatabase(wondersDao: WondersDao) : ListModel {
        return suspendCoroutine { continuation ->
            continuation.resume(ListModel(
                    wondersDao.getAllAncientWondersItems(),
                    wondersDao.getAllModernWondersItems(),
                    wondersDao.getAllNatureWondersItems()
            ))
        }
    }

    private suspend fun getDataFromNetwork() : DataModel {
        return suspendCoroutine { continuation ->
            NetworkService.getService().getCurrentData()
                .enqueue(object : Callback<DataModel>
                {
                    override fun onResponse(call: Call<DataModel>, response: Response<DataModel>) {

                        val result = response.body()

                        if (result != null)
                            continuation.resume(result)
                        else
                            continuation.resume(DataModel(ListModel(emptyList(), emptyList(), emptyList())))
                    }

                    override fun onFailure(call: Call<DataModel>, t: Throwable) {
                        t.printStackTrace()
                        continuation.resume(DataModel(ListModel(emptyList(), emptyList(), emptyList())))
                    }
                })
        }
    }

    override fun onItemClickEvent(itemModel: T) {
        val bundle = Bundle()
        var fragment: Fragment? = null
        when(itemModel) {
            is ListItemModel -> {
                bundle.putInt("id", itemModel.id)
                bundle.putString("name", itemModel.name)
                bundle.putString("description", itemModel.description)
                bundle.putString("location", itemModel.location)
                bundle.putString("date_cre", itemModel.creationDate)
                bundle.putString("date_des", itemModel.destructionDate)
                bundle.putDouble("latitude", itemModel.latitude)
                bundle.putDouble("longitude", itemModel.longitude)
                bundle.putString("url", itemModel.url)
                fragment = DetailedFragment()
            }
            is NatureWondersListItemModel -> {
                bundle.putInt("id", itemModel.id)
                bundle.putString("name", itemModel.name)
                bundle.putString("description", itemModel.description)
                bundle.putString("type", itemModel.type)
                bundle.putString("location", itemModel.location)
                bundle.putDouble("latitude", itemModel.latitude)
                bundle.putDouble("longitude", itemModel.longitude)
                bundle.putString("url", itemModel.url)
                fragment = NatureWondersDetailedFragment()
            }
        }
        fragment?.arguments = bundle
        if (fragment != null) {
            supportFragmentManager.beginTransaction().setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN).add(
                R.id.fragment_place, fragment).addToBackStack("detailed").commitAllowingStateLoss()
        }
    }

    override fun onTopButtonsClickEvent(buttonId: Int) {
        when(buttonId) {
            0 -> supportFragmentManager.beginTransaction().replace(R.id.fragment_place, ancientFragment!!).commitAllowingStateLoss()
            1 -> supportFragmentManager.beginTransaction().replace(R.id.fragment_place, modernFragment!!).commitAllowingStateLoss()
            2 -> supportFragmentManager.beginTransaction().replace(R.id.fragment_place, natureFragment!!).commitAllowingStateLoss()
        }
    }

    override fun onBackgroundClickEvent() {
        supportFragmentManager.popBackStack()
    }

    companion object {
        const val LAYOUT_ANCIENT = R.layout.ancient_wonders_list_item
        const val LAYOUT_MODERN = R.layout.modern_wonders_list_item
        const val LAYOUT_NATURE = R.layout.nature_wonders_list_item
        const val TAG = "MainActivity"
        const val OFFLINE_MODE = 0
        const val LOST_CONNECTION = 1
    }
}