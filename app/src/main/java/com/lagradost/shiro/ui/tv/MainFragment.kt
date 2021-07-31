package com.lagradost.shiro.ui.tv

import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.transition.AutoTransition
import androidx.transition.Transition
import androidx.transition.TransitionManager
import com.lagradost.shiro.R
import com.lagradost.shiro.ui.home.HomeFragment.Companion.homeViewModel
import com.lagradost.shiro.ui.home.HomeViewModel
import com.lagradost.shiro.ui.home.MasterCardAdapter
import com.lagradost.shiro.ui.library.LibraryFragment
import com.lagradost.shiro.ui.player.PlayerFragment.Companion.onPlayerNavigated
import com.lagradost.shiro.ui.result.ResultFragment.Companion.isInResults
import com.lagradost.shiro.ui.result.ResultFragment.Companion.onResultsNavigated
import com.lagradost.shiro.ui.settings.SettingsFragmentNew
import com.lagradost.shiro.utils.AppUtils.getColorFromAttr
import com.lagradost.shiro.utils.AppUtils.getCurrentActivity
import com.lagradost.shiro.utils.AppUtils.observe
import com.lagradost.shiro.utils.ShiroApi
import com.lagradost.shiro.utils.ShiroApi.Companion.initShiroApi
import com.lagradost.shiro.utils.ShiroApi.Companion.requestHome
import kotlinx.android.synthetic.main.fragment_main_tv.*
import kotlin.concurrent.thread


class MainFragment : Fragment() {

    private fun homeLoaded(data: ShiroApi.ShiroHomePage?) {
        activity?.runOnUiThread {
            main_load?.visibility = GONE
            main_reload_data_btt?.visibility = GONE
            vertical_grid_view.visibility = VISIBLE
            val adapter: RecyclerView.Adapter<RecyclerView.ViewHolder> = MasterCardAdapter(
                requireActivity(),
            )
            vertical_grid_view.adapter = adapter
            (vertical_grid_view.adapter as MasterCardAdapter).notifyDataSetChanged()
            //val snapHelper = LinearSnapHelper()
            //snapHelper.attachToRecyclerView(vertical_grid_view)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {

        homeViewModel =
            ViewModelProvider(getCurrentActivity()!!).get(HomeViewModel::class.java)
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main_tv, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        activity?.let {
            it.window.setBackgroundDrawable(ColorDrawable(it.getColorFromAttr(R.attr.background)))
        }
        ShiroApi.onHomeError += ::onHomeErrorCatch
        if (ShiroApi.hasThrownError != -1) {
            onHomeErrorCatch(ShiroApi.hasThrownError == 1)
        }


        val focusListener = View.OnFocusChangeListener { v, hasFocus ->
            val transition: Transition = AutoTransition()
            transition.duration = 2000 // DURATION OF ANIMATION IN MS
            tv_menu_bar?.let {
                TransitionManager.beginDelayedTransition(it, transition)
            }
            val scale = if (hasFocus) 0.7f else 0.5f
            v?.scaleX = scale
            v?.scaleY = scale
        }

        search_icon.onFocusChangeListener = focusListener
        settings_icon.onFocusChangeListener = focusListener
        library_icon.onFocusChangeListener = focusListener

        /*settings_button.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.main_browse_fragment, SettingsFragment())
                ?.commit()
        }*/
        tv_menu_bar.visibility = VISIBLE

        search_icon.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.home_root_tv, SearchFragmentTv())
                ?.commit()
        }
        library_icon.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.home_root_tv, LibraryFragment())
                ?.commit()
        }
        settings_icon.setOnClickListener {
            activity?.supportFragmentManager?.beginTransaction()
                ?.replace(R.id.home_root_tv, SettingsFragmentNew())
                ?.commit()
        }
        homeViewModel?.apiData?.observe(viewLifecycleOwner) {
            homeLoaded(it)
        }
    }

    private fun restoreState(hasEntered: Boolean) {
        if (hasEntered) {
            // Needed to prevent focus when on bottom
            view?.visibility = GONE
        } else {
            if (isInResults) return
            view?.visibility = VISIBLE
            // Somehow fucks up if you've been in player, I've yet to understand why
            if (hasBeenInPlayer) {
                hasBeenInPlayer = false
                activity?.supportFragmentManager
                    ?.beginTransaction()
                    ?.detach(this)
                    ?.attach(this)
                    ?.commitAllowingStateLoss()
            }
        }
    }

    private fun onHomeErrorCatch(fullRe: Boolean) {
        // Null check because somehow this can crash
        activity?.runOnUiThread {
            // ?. because it somehow crashes anyways without it for one person
            if (main_reload_data_btt != null) {
                main_reload_data_btt?.visibility = VISIBLE
                main_load?.visibility = GONE
                main_reload_data_btt?.setOnClickListener {
                    main_reload_data_btt?.visibility = GONE
                    main_load?.visibility = VISIBLE
                    thread {
                        if (fullRe) {
                            context?.initShiroApi()
                        } else {
                            context?.requestHome(false)
                        }
                    }
                }
            }
        }
    }

    override fun onResume() {

        observe(homeViewModel!!.subscribed) {
            (vertical_grid_view?.adapter as? MasterCardAdapter)?.notifyDataSetChanged()
        }
        observe(homeViewModel!!.favorites) {
            (vertical_grid_view?.adapter as? MasterCardAdapter)?.notifyDataSetChanged()
        }
        context?.requestHome()
        onResultsNavigated += ::restoreState
        onPlayerNavigated += ::restoreState
        homeViewModel!!.apiData.observe(viewLifecycleOwner) {
            homeLoaded(it)
        }
        super.onResume()
    }

    override fun onDestroy() {
        onResultsNavigated -= ::restoreState
        onPlayerNavigated -= ::restoreState
        super.onDestroy()
    }

    companion object {
        var hasBeenInPlayer: Boolean = false
        fun newInstance() =
            MainFragment().apply {
                arguments = Bundle().apply {
                }
            }
    }
}