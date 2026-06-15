// pages/service/evaluation-list/evaluation-list.js
const api = require('../../../utils/api')

Page({
    data: {
        // 统计信息（来自 /stats/caregiver/my 接口）
        averageRating: 0,
        reviewCount: 0,
        goodReviewRate: 0,

        // 列表数据
        allReviewList: [],  // 全量评价数据
        reviewList: [],     // 当前展示的列表（按标签过滤后）
        loading: false,
        hasMore: true,
        page: 1,
        pageSize: 10,
        activeTab: 'all'
    },

    onLoad() {
        this.loadStats()
        this.loadReviewList(true)
    },

    // 加载统计信息（/stats/caregiver/my 接口）
    async loadStats() {
        try {
            const stats = await api.stats.getMyStats()
            this.setData({
                averageRating: stats.averageRating || 0,
                reviewCount: stats.reviewCount || 0,
                goodReviewRate: stats.goodReviewRate || 0
            })
        } catch (err) {
            console.error('加载统计信息失败:', err)
        }
    },

    // 加载评价列表（/review/caregiver/my-list 接口）
    async loadReviewList(isRefresh = false) {
        if (this.data.loading) return
        if (!isRefresh && !this.data.hasMore) return

        this.setData({ loading: true })

        try {
            const page = isRefresh ? 1 : this.data.page
            const params = {
                page: page,
                size: this.data.pageSize
            }

            const res = await api.review.getMyReviews(params)
            const records = res.records || []
            const newList = Array.isArray(records) ? records : []

            // 累加全量数据
            const allReviewList = isRefresh ? newList : [...this.data.allReviewList, ...newList]

            // 按当前标签过滤
            const reviewList = this.filterByTab(allReviewList, this.data.activeTab)

            this.setData({
                allReviewList,
                reviewList,
                page: page + 1,
                hasMore: newList.length >= this.data.pageSize,
                loading: false
            })
        } catch (err) {
            console.error('加载评价列表失败:', err)
            this.setData({ loading: false })
        }
    },

    // 按标签过滤评价列表
    filterByTab(list, tab) {
        if (tab === 'good') {
            return list.filter(item => item.type === 1)
        }
        if (tab === 'bad') {
            return list.filter(item => item.type === 2)
        }
        return list
    },

    // 切换标签（客户端过滤，后端接口不支持 type 参数筛选）
    onTabChange(e) {
        const activeTab = e.detail.name
        const reviewList = this.filterByTab(this.data.allReviewList, activeTab)
        this.setData({
            activeTab,
            reviewList
        })
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.loadStats()
        this.loadReviewList(true)
        setTimeout(() => {
            wx.stopPullDownRefresh()
        }, 1000)
    },

    // 触底加载
    onReachBottom() {
        this.loadReviewList(false)
    }
})
