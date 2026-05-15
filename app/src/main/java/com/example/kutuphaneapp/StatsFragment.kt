package com.example.kutuphaneapp

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import com.bumptech.glide.Glide
import com.example.kutuphaneapp.model.Book
import com.example.kutuphaneapp.util.StatsUtil
import com.example.kutuphaneapp.viewmodel.StatsViewModel
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.charts.PieChart
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import com.github.mikephil.charting.data.PieData
import com.github.mikephil.charting.data.PieDataSet
import com.github.mikephil.charting.data.PieEntry
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter
import com.google.android.material.progressindicator.CircularProgressIndicator
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale
import kotlin.math.roundToInt

class StatsFragment : Fragment() {

    private val viewModel: StatsViewModel by viewModels()

    private lateinit var progressBar: CircularProgressIndicator
    private lateinit var statsScroll: View

    // Bölüm 1
    private lateinit var cpiGoal: CircularProgressIndicator
    private lateinit var tvGoalPercent: TextView
    private lateinit var tvGoalRatio: TextView
    private lateinit var tvTotalPagesRead: TextView
    private lateinit var tvAvgBookLength: TextView
    private lateinit var pieChart: PieChart

    // Bölüm 2
    private lateinit var ivThickest: ImageView
    private lateinit var ivShortest: ImageView
    private lateinit var ivOldestPub: ImageView
    private lateinit var ivFirstAdded: ImageView
    private lateinit var tvThickest: TextView
    private lateinit var tvShortest: TextView
    private lateinit var tvOldestPub: TextView
    private lateinit var tvFirstAdded: TextView
    private lateinit var tvThickestDetail: TextView
    private lateinit var tvShortestDetail: TextView
    private lateinit var tvOldestPubDetail: TextView
    private lateinit var tvFirstAddedDetail: TextView

    // Bölüm 3
    private lateinit var tvFavoriteAuthor: TextView
    private lateinit var tvFavoritePublisher: TextView
    private lateinit var chipGroupTagsStats: com.google.android.material.chip.ChipGroup
    private lateinit var layoutMasterpiece: View
    private lateinit var ivMasterpiece: ImageView
    private lateinit var tvMasterpiece: TextView

    // Bölüm 4
    private lateinit var monthlyBarChart: BarChart
    private lateinit var tvAvgFinishDays: TextView
    private lateinit var tvDustyShelf: TextView
    private lateinit var tvDailySpeed: TextView

    // Bölüm 5
    private lateinit var tvRating1: TextView
    private lateinit var tvRating2: TextView
    private lateinit var tvRating3: TextView
    private lateinit var tvRating4: TextView
    private lateinit var tvRating5: TextView
    private lateinit var piRating1: LinearProgressIndicator
    private lateinit var piRating2: LinearProgressIndicator
    private lateinit var piRating3: LinearProgressIndicator
    private lateinit var piRating4: LinearProgressIndicator
    private lateinit var piRating5: LinearProgressIndicator
    private lateinit var tvAuthorDiversity: TextView
    private lateinit var tvCompletionRate: TextView
    private lateinit var tvStreak: TextView

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_stats, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initViews(view)
        observeViewModel()
        viewModel.fetchStats()
    }

    private fun initViews(view: View) {
        progressBar = view.findViewById(R.id.progress_bar_stats)
        statsScroll = view.findViewById(R.id.scroll_stats)

        cpiGoal = view.findViewById(R.id.cpi_goal)
        tvGoalPercent = view.findViewById(R.id.tv_goal_percent)
        tvGoalRatio = view.findViewById(R.id.tv_goal_ratio)
        tvTotalPagesRead = view.findViewById(R.id.tv_total_pages_read)
        tvAvgBookLength = view.findViewById(R.id.tv_avg_book_length)
        pieChart = view.findViewById(R.id.chart_status_pie)

        ivThickest = view.findViewById(R.id.iv_thickest)
        ivShortest = view.findViewById(R.id.iv_shortest)
        ivOldestPub = view.findViewById(R.id.iv_oldest_pub)
        ivFirstAdded = view.findViewById(R.id.iv_first_added)
        tvThickest = view.findViewById(R.id.tv_thickest)
        tvShortest = view.findViewById(R.id.tv_shortest)
        tvOldestPub = view.findViewById(R.id.tv_oldest_pub)
        tvFirstAdded = view.findViewById(R.id.tv_first_added)

        tvThickestDetail = view.findViewById(R.id.tv_thickest_detail)
        tvShortestDetail = view.findViewById(R.id.tv_shortest_detail)
        tvOldestPubDetail = view.findViewById(R.id.tv_oldest_pub_detail)
        tvFirstAddedDetail = view.findViewById(R.id.tv_first_added_detail)

        tvFavoriteAuthor = view.findViewById(R.id.tv_favorite_author)
        tvFavoritePublisher = view.findViewById(R.id.tv_favorite_publisher)
        chipGroupTagsStats = view.findViewById(R.id.chip_group_tags_stats)
        layoutMasterpiece = view.findViewById(R.id.layout_masterpiece)
        ivMasterpiece = view.findViewById(R.id.iv_masterpiece)
        tvMasterpiece = view.findViewById(R.id.tv_masterpiece)

        monthlyBarChart = view.findViewById(R.id.chart_monthly_bar)
        tvAvgFinishDays = view.findViewById(R.id.tv_avg_finish_days)
        tvDustyShelf = view.findViewById(R.id.tv_dusty_shelf)
        tvDailySpeed = view.findViewById(R.id.tv_daily_speed)

        tvRating1 = view.findViewById(R.id.tv_rating_1)
        tvRating2 = view.findViewById(R.id.tv_rating_2)
        tvRating3 = view.findViewById(R.id.tv_rating_3)
        tvRating4 = view.findViewById(R.id.tv_rating_4)
        tvRating5 = view.findViewById(R.id.tv_rating_5)
        piRating1 = view.findViewById(R.id.pi_rating_1)
        piRating2 = view.findViewById(R.id.pi_rating_2)
        piRating3 = view.findViewById(R.id.pi_rating_3)
        piRating4 = view.findViewById(R.id.pi_rating_4)
        piRating5 = view.findViewById(R.id.pi_rating_5)
        tvAuthorDiversity = view.findViewById(R.id.tv_author_diversity)
        tvCompletionRate = view.findViewById(R.id.tv_completion_rate)
        tvStreak = view.findViewById(R.id.tv_streak)
    }

    private fun observeViewModel() {
        viewModel.stats.observe(viewLifecycleOwner) { stats ->
            renderStats(stats)
        }

        viewModel.isLoading.observe(viewLifecycleOwner) { isLoading ->
            progressBar.visibility = if (isLoading) View.VISIBLE else View.GONE
            statsScroll.visibility = if (isLoading) View.GONE else View.VISIBLE
        }

        viewModel.error.observe(viewLifecycleOwner) { error ->
            error?.let {
                Toast.makeText(requireContext(), it, Toast.LENGTH_LONG).show()
                viewModel.clearError()
            }
        }
    }

    private fun renderStats(stats: StatsUtil.StatsData) {
        val goalPercent = if (stats.readingGoal <= 0) 0 else ((stats.readCount * 100f / stats.readingGoal).roundToInt()).coerceIn(0, 100)
        cpiGoal.max = 100
        cpiGoal.progress = goalPercent
        tvGoalPercent.text = "%$goalPercent"
        tvGoalRatio.text = "${stats.readCount} / ${stats.readingGoal} kitap"

        tvTotalPagesRead.text = "${stats.totalReadPages} sayfa"
        tvAvgBookLength.text = "Ortalama ${stats.averageBookLength} sayfalık kitaplar okuyorsun."

        setupStatusPieChart(stats.readCount, stats.readingCount, stats.toReadCount)
        
        bindMiniBookCard(stats.thickestBook, ivThickest, tvThickest, tvThickestDetail) { it.pageCount.toString() + " sayfa" }
        bindMiniBookCard(stats.shortestBook, ivShortest, tvShortest, tvShortestDetail) { it.pageCount.toString() + " sayfa" }
        bindMiniBookCard(stats.oldestPublishedBook, ivOldestPub, tvOldestPub, tvOldestPubDetail) { "Yıl: " + (StatsUtil.extractYear(it.publishedDate) ?: "-") }
        bindMiniBookCard(stats.firstAddedBook, ivFirstAdded, tvFirstAdded, tvFirstAddedDetail) { 
            val format = java.text.SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            "Eklenme: " + format.format(java.util.Date(it.addedAt))
        }

        tvFavoriteAuthor.text = "${stats.favoriteAuthor?.key ?: "-"} (${stats.favoriteAuthor?.value ?: 0} kitap)"
        tvFavoritePublisher.text = "${stats.favoritePublisher?.key ?: "-"} (${stats.favoritePublisher?.value ?: 0} kitap)"
        
        chipGroupTagsStats.removeAllViews()
        if (stats.topTags.isEmpty()) {
            val emptyChip = com.google.android.material.chip.Chip(requireContext())
            emptyChip.text = "Henüz etiket yok"
            emptyChip.isClickable = false
            chipGroupTagsStats.addView(emptyChip)
        } else {
            stats.topTags.forEach { tag ->
                val chip = com.google.android.material.chip.Chip(requireContext())
                chip.text = tag
                chip.isClickable = false
                chip.setChipBackgroundColorResource(R.color.surface_variant)
                chip.setTextColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
                chipGroupTagsStats.addView(chip)
            }
        }

        if (stats.randomMasterpiece != null) {
            layoutMasterpiece.visibility = View.VISIBLE
            tvMasterpiece.text = stats.randomMasterpiece.title
            loadCover(stats.randomMasterpiece.coverUrl, ivMasterpiece)
        } else {
            layoutMasterpiece.visibility = View.GONE
        }

        setupMonthlyBarChart(stats.monthlyFinished)
        tvAvgFinishDays.text = "Ortalama Bitirme Süresi: ${stats.avgFinishDays} gün"
        tvDustyShelf.text = "Tozlu Raflar: ${stats.dustyShelfBook?.title ?: "Bekleyen kitap yok"}"
        tvDailySpeed.text = "Günlük Okuma Hızı: ${"%.1f".format(Locale("tr"), stats.dailySpeed)} sayfa/gün"

        bindRatingProgress(stats.ratingCounts)
        tvAuthorDiversity.text = "Yazarlarının %${stats.authorDiversityPercent}'i birbirinden farklı."
        tvCompletionRate.text = "Tamamlama Oranı: %${stats.completionPercent}"
        tvStreak.text = "Okuma Serisi: ${stats.streakMonths} ay kesintisiz bitiriş"
    }

    private fun setupStatusPieChart(read: Int, reading: Int, toRead: Int) {
        val entries = mutableListOf<PieEntry>()
        if (read > 0) entries.add(PieEntry(read.toFloat(), "Okudum"))
        if (reading > 0) entries.add(PieEntry(reading.toFloat(), "Okuyorum"))
        if (toRead > 0) entries.add(PieEntry(toRead.toFloat(), "Okuyacağım"))

        val set = PieDataSet(entries, "").apply {
            colors = listOf(
                ContextCompat.getColor(requireContext(), R.color.success),
                ContextCompat.getColor(requireContext(), R.color.info),
                ContextCompat.getColor(requireContext(), R.color.accent)
            )
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            valueTextSize = 14f
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return value.toInt().toString()
                }
            }
        }
        pieChart.apply {
            data = PieData(set)
            setUsePercentValues(false)
            description.isEnabled = false
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            legend.horizontalAlignment = com.github.mikephil.charting.components.Legend.LegendHorizontalAlignment.CENTER
            setHoleColor(android.graphics.Color.TRANSPARENT)
            setEntryLabelColor(ContextCompat.getColor(requireContext(), R.color.text_primary))
            setEntryLabelTextSize(12f)
            invalidate()
        }
    }

    private fun setupMonthlyBarChart(monthlyFinished: IntArray) {
        val monthNames = listOf("Oca", "Şub", "Mar", "Nis", "May", "Haz", "Tem", "Ağu", "Eyl", "Eki", "Kas", "Ara")
        val entries = monthlyFinished.mapIndexed { index, value -> BarEntry(index.toFloat(), value.toFloat()) }
        val set = BarDataSet(entries, "Bitirilen Kitap").apply {
            color = ContextCompat.getColor(requireContext(), R.color.accent)
            valueTextColor = ContextCompat.getColor(requireContext(), R.color.text_primary)
            valueTextSize = 10f
            valueFormatter = object : com.github.mikephil.charting.formatter.ValueFormatter() {
                override fun getFormattedValue(value: Float): String {
                    return if (value > 0) value.toInt().toString() else ""
                }
            }
        }
        monthlyBarChart.apply {
            data = BarData(set).apply { barWidth = 0.65f }
            description.isEnabled = false
            axisRight.isEnabled = false
            axisLeft.apply {
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                axisMinimum = 0f
                granularity = 1f
            }
            xAxis.apply {
                position = XAxis.XAxisPosition.BOTTOM
                textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
                valueFormatter = IndexAxisValueFormatter(monthNames)
                granularity = 1f
                setDrawGridLines(false)
            }
            legend.textColor = ContextCompat.getColor(requireContext(), R.color.text_secondary)
            animateY(700)
            invalidate()
        }
    }

    private fun bindMiniBookCard(book: Book?, imageView: ImageView, tvTitle: TextView, tvDetail: TextView, getDetail: (Book) -> String) {
        if (book == null) {
            tvTitle.text = "-"
            tvDetail.text = "-"
            loadCover(null, imageView)
            return
        }
        tvTitle.text = book.title
        tvDetail.text = getDetail(book)
        loadCover(book.coverUrl, imageView)
    }

    private fun loadCover(url: String?, imageView: ImageView) {
        if (!isAdded) return
        Glide.with(this)
            .load(url?.takeIf { it.isNotBlank() })
            .placeholder(R.drawable.ic_launcher_foreground)
            .error(R.drawable.ic_launcher_foreground)
            .into(imageView)
    }

    private fun bindRatingProgress(ratingCounts: IntArray) {
        val totalRated = ratingCounts.sum().coerceAtLeast(1)
        val bars = listOf(piRating1, piRating2, piRating3, piRating4, piRating5)
        val texts = listOf(tvRating1, tvRating2, tvRating3, tvRating4, tvRating5)
        for (i in 0 until 5) {
            val count = ratingCounts[i]
            val percent = (count * 100 / totalRated)
            bars[i].max = 100
            bars[i].progress = percent
            texts[i].text = "${i + 1} Yıldız: $count"
        }
    }
}
