package com.lagradost.shiro.ui.downloads

import DOWNLOAD_PARENT_KEY
import DataStore.containsKey
import DataStore.getKey
import DataStore.removeKey
import DataStore.setKey
import VIEWSTATE_KEY
import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.view.ContextThemeWrapper
import androidx.appcompat.widget.LinearLayoutCompat
import androidx.fragment.app.Fragment
import com.jaredrummler.cyanea.Cyanea
import com.lagradost.shiro.R
import com.lagradost.shiro.ui.MainActivity
import com.lagradost.shiro.ui.downloads.DownloadFragment.Companion.downloadsUpdated
import com.lagradost.shiro.ui.home.ExpandedHomeFragment.Companion.isInExpandedView
import com.lagradost.shiro.ui.player.PlayerData
import com.lagradost.shiro.ui.player.PlayerFragment
import com.lagradost.shiro.ui.player.PlayerFragment.Companion.isInPlayer
import com.lagradost.shiro.ui.result.ResultFragment.Companion.fixEpTitle
import com.lagradost.shiro.ui.result.ResultFragment.Companion.isInResults
import com.lagradost.shiro.ui.result.ResultFragment.Companion.isViewState
import com.lagradost.shiro.utils.AppUtils.getCurrentActivity
import com.lagradost.shiro.utils.AppUtils.getTextColor
import com.lagradost.shiro.utils.AppUtils.getViewKey
import com.lagradost.shiro.utils.AppUtils.getViewPosDur
import com.lagradost.shiro.utils.AppUtils.loadPlayer
import com.lagradost.shiro.utils.AppUtils.popCurrentPage
import com.lagradost.shiro.utils.AppUtils.settingsManager
import com.lagradost.shiro.utils.DownloadManager
import com.lagradost.shiro.utils.VideoDownloadManager
import com.lagradost.shiro.utils.VideoDownloadManager.downloadStatus
import kotlinx.android.synthetic.main.episode_result_downloaded.view.*
import kotlinx.android.synthetic.main.fragment_download_child.*

const val SLUG = "slug"

class DownloadFragmentChild : Fragment() {
    var slug: String? = null
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        isInResults = true
        arguments?.getString(SLUG)?.let {
            slug = it
        }
        download_child_scroll_view.background = ColorDrawable(Cyanea.instance.backgroundColor)
        val topParams: LinearLayout.LayoutParams = LinearLayout.LayoutParams(
            LinearLayoutCompat.LayoutParams.MATCH_PARENT, // view width
            MainActivity.statusHeight // view height
        )
        top_padding_download_child?.layoutParams = topParams
        PlayerFragment.onPlayerNavigated += ::onPlayerLeft
        download_go_back?.setOnClickListener {
            activity?.popCurrentPage(isInPlayer, isInExpandedView, isInResults)
        }
        loadData()
    }

    override fun onDestroy() {
        super.onDestroy()
        PlayerFragment.onPlayerNavigated -= ::onPlayerLeft
        isInResults = false
    }

    private fun onPlayerLeft(it: Boolean) {
        loadData()
    }

    @SuppressLint("ClickableViewAccessibility", "SetTextI18n")
    private fun loadData() {
        downloadRootChild.removeAllViews()
        val save = settingsManager!!.getBoolean("save_history", true)

        // When fastani is down it doesn't report any seasons and this is needed.
        val parent = context?.getKey<DownloadManager.DownloadParentFileMetadata>(DOWNLOAD_PARENT_KEY, slug!!)
        download_header_text?.text = parent?.title
        // Sorts by Seasons and Episode Index

        val sortedEpisodeKeys = context?.getAllDownloadedEpisodes(slug!!)

        sortedEpisodeKeys?.forEach { it ->
            val child = it.key

            if (child != null) {
                // val file = File(child.videoPath)
                val fileInfo = VideoDownloadManager.getDownloadFileInfoAndUpdateSettings(
                    requireContext(),
                    child.internalId
                ) ?: return@forEach

                val card: View =
                    layoutInflater.inflate(R.layout.episode_result_downloaded, view?.parent as? ViewGroup?, false)
                /*if (child.thumbPath != null) {
                    card.imageView?.setImageURI(Uri.parse(child.thumbPath))
                }*/

                /*
                fun showMoveButton() {
                    if (child.videoPath.startsWith(getCurrentActivity()!!.filesDir.toString())) {
                        card.switch_storage_button.visibility = VISIBLE
                        card.switch_storage_button.setOnClickListener { switchStorageButton ->
                            activity?.let { activity ->
                                if (!activity.checkWrite()) {
                                    Toast.makeText(
                                        activity,
                                        "Accept storage permissions to move to external storage",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    activity.requestRW()
                                    return@setOnClickListener
                                }
                                val builder = AlertDialog.Builder(activity, R.style.AlertDialogCustom)
                                builder.apply {
                                    setPositiveButton(
                                        "OK"
                                    ) { _, _ ->
                                        thread {
                                            val result = moveToExternalStorage(child)
                                            activity.runOnUiThread {
                                                if (result) {
                                                    Toast.makeText(
                                                        activity,
                                                        "Moved ${child.videoTitle} to external storage",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                    switchStorageButton?.visibility = INVISIBLE
                                                } else {
                                                    Toast.makeText(
                                                        activity,
                                                        "Failed to move to external storage",
                                                        Toast.LENGTH_SHORT
                                                    ).show()
                                                }
                                            }
                                        }
                                    }
                                    setNegativeButton(
                                        "Cancel"
                                    ) { _, _ ->
                                        // User cancelled the dialog
                                    }
                                }
                                builder.setTitle("Move to external storage")
                                // Create the AlertDialog
                                builder.create()
                                builder.show()
                            }
                        }
                    } else {
                        card.switch_storage_button.visibility = INVISIBLE
                    }
                }*/

                val episodeOffset =
                    if (child.animeData.episodes?.filter { it.episode_number == 0 }.isNullOrEmpty()) 0 else -1

                val key = getViewKey(slug!!, child.episodeIndex)
                card.cardBg.setOnClickListener {
                    if (save) {
                        context?.setKey(VIEWSTATE_KEY, key, System.currentTimeMillis())
                    }
                    activity?.loadPlayer(
                        PlayerData(
                            "Episode ${child.episodeIndex + 1 + episodeOffset} · ${child.videoTitle}",
                            fileInfo.path.toString(),// child.videoPath,
                            child.episodeIndex,
                            0,
                            null,
                            null,
                            slug!!,
                            parent?.anilistID,
                            parent?.malID,
                            parent?.fillerEpisodes
                        )
                    )
                }


                //MainActivity.loadPlayer(epIndex, index, data)
                val title = fixEpTitle(
                    child.videoTitle, child.episodeIndex + 1 + episodeOffset,
                    parent?.isMovie == true, true
                )

                // ================ DOWNLOAD STUFF ================
                fun deleteFile() {
                    println("FIXED:::: " + child.internalId)
                    if (VideoDownloadManager.deleteFileAndUpdateSettings(requireContext(), child.internalId)) {
                        activity?.runOnUiThread {
                            card.visibility = GONE
                            context?.removeKey(it.value)
                            Toast.makeText(
                                context,
                                "${child.videoTitle} E${child.episodeIndex + 1 + episodeOffset} deleted",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                        downloadsUpdated.invoke(true)
                    }
                }

                card.cardRemoveIcon.setOnClickListener {
                    val alertDialog: AlertDialog? = activity?.let {
                        val builder = AlertDialog.Builder(it, R.style.AlertDialogCustom)
                        builder.apply {
                            setPositiveButton(
                                "Delete"
                            ) { _, _ ->
                                deleteFile()
                            }
                            setNegativeButton(
                                "Cancel"
                            ) { _, _ ->
                                // User cancelled the dialog
                            }
                        }
                        // Set other dialog properties
                        builder.setTitle("Delete ${child.videoTitle} - E${child.episodeIndex + 1 + episodeOffset}")

                        // Create the AlertDialog
                        builder.create()
                    }
                    alertDialog?.show()
                }

                card.setOnLongClickListener {
                    if (isViewState) {
                        if (context?.containsKey(VIEWSTATE_KEY, key) == true) {
                            context?.removeKey(VIEWSTATE_KEY, key)
                        } else {
                            context?.setKey(VIEWSTATE_KEY, key, System.currentTimeMillis())
                        }
                        loadData()
                    }
                    return@setOnLongClickListener true
                }

                val fillerInfo =
                    if (parent?.fillerEpisodes?.get(child.episodeIndex + 1) == true) " (Filler) " else ""

                card.cardTitle.text = title + fillerInfo
                val megaBytesTotal = DownloadManager.convertBytesToAny(fileInfo.totalBytes, 0, 2.0).toInt()
                val localBytesTotal = maxOf(DownloadManager.convertBytesToAny(fileInfo.fileLength, 0, 2.0).toInt(), 1)
                card.cardTitleExtra.text = "$localBytesTotal / $megaBytesTotal MB"
                card.progressBar.progressTintList = ColorStateList.valueOf(Cyanea.instance.primary)
                fun updateIcon(megabytes: Int) {
                    if (megabytes + 0.1 >= megaBytesTotal) {
                        card.progressBar.visibility = GONE
                        card.cardPauseIcon.visibility = GONE
                        card.cardRemoveIcon.visibility = VISIBLE
                        //   showMoveButton()
                    } else {
                        card.progressBar.visibility = VISIBLE
                        card.cardRemoveIcon.visibility = GONE
                        card.cardPauseIcon.visibility = VISIBLE
                    }
                }

                fun getDownload(): DownloadManager.DownloadInfo {
                    return DownloadManager.DownloadInfo(
                        child.episodeIndex,
                        child.animeData,
                        parent?.anilistID,
                        parent?.malID,
                        parent?.fillerEpisodes
                    )
                }

                fun getStatus(): Boolean { // IF CAN RESUME
                    return if (downloadStatus.containsKey(child.internalId)) {
                        downloadStatus[child.internalId] == VideoDownloadManager.DownloadType.IsPaused
                    } else {
                        true
                    }
                }

                fun setStatus() {
                    activity?.runOnUiThread {
                        if (getStatus()) {
                            card.cardPauseIcon.setImageResource(R.drawable.netflix_play)
                        } else {
                            card.cardPauseIcon.setImageResource(R.drawable.exo_icon_stop)
                        }
                    }
                }

                setStatus()
                updateIcon(localBytesTotal)
                card.cardPauseIcon.imageTintList = ColorStateList.valueOf(Cyanea.instance.primary)
                card.cardPauseIcon.setOnClickListener { v ->
                    val ctw = ContextThemeWrapper(context, R.style.PopupMenu)
                    val popup = PopupMenu(ctw, v)
                    if (getStatus()) {
                        popup.setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.res_resumedload -> {
                                    if (downloadStatus.containsKey(child.internalId)) {
                                        VideoDownloadManager.downloadEvent.invoke(
                                            Pair(
                                                child.internalId,
                                                VideoDownloadManager.DownloadActionType.Resume
                                            )
                                        )
                                    }
                                    val pkg = VideoDownloadManager.getDownloadResumePackage(
                                        requireContext(),
                                        child.internalId
                                    )
                                    if (pkg != null) {
                                        VideoDownloadManager.downloadFromResume(requireContext(), pkg)
                                    }

                                }
                                R.id.res_stopdload -> {
                                    VideoDownloadManager.downloadEvent.invoke(
                                        Pair(
                                            child.internalId,
                                            VideoDownloadManager.DownloadActionType.Stop
                                        )
                                    )
                                    deleteFile()
                                }
                            }
                            return@setOnMenuItemClickListener true
                        }
                        popup.inflate(R.menu.resume_menu)
                    } else {
                        popup.setOnMenuItemClickListener {
                            when (it.itemId) {
                                R.id.stop_pauseload -> {
                                    VideoDownloadManager.downloadEvent.invoke(
                                        Pair(
                                            child.internalId,
                                            VideoDownloadManager.DownloadActionType.Pause
                                        )
                                    )
                                }
                                R.id.stop_stopdload -> {
                                    VideoDownloadManager.downloadEvent.invoke(
                                        Pair(
                                            child.internalId,
                                            VideoDownloadManager.DownloadActionType.Stop
                                        )
                                    )
                                    deleteFile()
                                }
                            }
                            return@setOnMenuItemClickListener true
                        }
                        popup.inflate(R.menu.stop_menu)
                    }
                    popup.show()
                }

                card.progressBar.progress = maxOf(minOf(localBytesTotal * 100 / megaBytesTotal, 100), 0)

                VideoDownloadManager.downloadProgressEvent += {
                    activity?.runOnUiThread {
                        if (it.first == child.internalId) {
                            val megaBytes =
                                DownloadManager.convertBytesToAny(it.second, 0, 2.0).toInt()
                            card.cardTitleExtra.text = "$megaBytes / $megaBytesTotal MB"
                            card.progressBar.progress = maxOf(minOf(megaBytes * 100 / megaBytesTotal, 100), 0)
                            updateIcon(megaBytes)
                            setStatus()
                        }
                    }
                }

                // ================ REGULAR ================
                if (context?.containsKey(VIEWSTATE_KEY, key) == true) {
                    card.cardBg.setCardBackgroundColor(
                        Cyanea.instance.primaryDark
                    )
                    card.cardTitle.setTextColor(
                        getCurrentActivity()!!.getTextColor()
                    )
                    card.cardTitleExtra.setTextColor(
                        getCurrentActivity()!!.getTextColor()
                    )
                } else {
                    card.cardBg.setCardBackgroundColor(
                        Cyanea.instance.backgroundColorDark
                    )
                    card.cardTitle.setTextColor(
                        getCurrentActivity()!!.getTextColor()
                    )
                    card.cardTitleExtra.setTextColor(
                        getCurrentActivity()!!.getTextColor()
                    )
                }

                val pro = context?.getViewPosDur(slug!!, child.episodeIndex)
                if (pro != null) {
                    if (pro.dur > 0 && pro.pos > 0) {
                        var progress: Int = (pro.pos * 100L / pro.dur).toInt()
                        if (progress < 5) {
                            progress = 5
                        } else if (progress > 95) {
                            progress = 100
                        }
                        card.video_progress.progress = progress
                    } else {
                        card.video_progress?.alpha = 0f
                    }
                } else {
                    card.video_progress?.alpha = 0f
                }
                downloadRootChild.addView(card)
                downloadRootChild.invalidate()
            }
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_download_child, container, false)
    }


    companion object {
        fun Context.getAllDownloadedEpisodes(slug: String): Map<DownloadManager.DownloadFileMetadata?, String> {
            // When shiro is down it doesn't report any seasons and this is needed.
            val episodeKeys = DownloadFragment.childMetadataKeys[slug]
            //val parent = DataStore.getKey<DownloadManager.DownloadParentFileMetadata>(DOWNLOAD_PARENT_KEY, slug!!)
            // Sorts by Seasons and Episode Index

            return episodeKeys?.associateBy<String, DownloadManager.DownloadFileMetadata?, String>({ key ->
                getKey(key)
            }, { it })?.toList()
                ?.sortedBy { (key, _) -> key?.episodeIndex }?.toMap() ?: mapOf()
        }

        fun newInstance(slug: String) =
            DownloadFragmentChild().apply {
                arguments = Bundle().apply {
                    putString(SLUG, slug)
                }
            }
    }
}