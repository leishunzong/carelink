const api = require('../../../utils/api')

const PACKAGE_CATEGORIES = [
    { id: 0, name: '全部' },
    { id: 1, name: '居家陪护' },
    { id: 2, name: '医院陪护' },
    { id: 3, name: '周期护理' },
    { id: 4, name: '家政服务' },
    { id: 5, name: '陪诊服务' },
    { id: 6, name: '母婴护理' }
]

Page({
    data: {
        categories: PACKAGE_CATEGORIES,
        activeCategory: 0,
        packageList: [],
        loading: false,
        page: 1,
        pageSize: 10,
        hasMore: true
    },

    onLoad() {
        this.loadPackages(true)
    },

    onShow() {
        this.loadPackages(true)
    },

    // 调用 /package/available 接口，后端直接返回带开通状态的服务包列表
    async loadPackages(isRefresh = false) {
        if (this.data.loading) return
        if (!isRefresh && !this.data.hasMore) return

        this.setData({ loading: true })

        try {
            const page = isRefresh ? 1 : this.data.page
            const params = {
                current: page,
                size: this.data.pageSize
            }

            const { activeCategory } = this.data
            if (activeCategory > 0) {
                params.category = activeCategory
            }

            const res = await api.servicePackage.getAvailableWithStatus(params)
            const records = res.records || []

            const packageList = isRefresh ? records : [...this.data.packageList, ...records]

            this.setData({
                packageList,
                page: page + 1,
                hasMore: records.length >= this.data.pageSize,
                loading: false
            })
        } catch (err) {
            console.error('加载服务包列表失败:', err)
            this.setData({ loading: false })
        }
    },

    // 切换分类
    onCategoryChange(e) {
        const id = parseInt(e.currentTarget.dataset.id)
        this.setData({
            activeCategory: id,
            packageList: [],
            page: 1,
            hasMore: true
        })
        this.loadPackages(true)
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.loadPackages(true)
        setTimeout(() => {
            wx.stopPullDownRefresh()
        }, 1000)
    },

    // 触底加载更多
    onReachBottom() {
        this.loadPackages(false)
    },

    // 开通服务包
    handleOpenPackage(e) {
        const { id, name } = e.currentTarget.dataset
        console.log('[开通服务包] packageId:', id, ', name:', name)

        wx.showModal({
            title: '开通服务包',
            content: `确定要开通「${name}」吗？`,
            confirmText: '确认开通',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        wx.showLoading({ title: '开通中...', mask: true })
                        const result = await api.servicePackage.open(id)
                        console.log('[开通服务包] 成功:', result)
                        wx.hideLoading()

                        wx.showToast({
                            title: '开通成功',
                            icon: 'success'
                        })

                        this.loadPackages(true)
                    } catch (err) {
                        wx.hideLoading()
                        console.error('[开通服务包] 失败:', err)
                        wx.showToast({
                            title: err.message || '开通失败',
                            icon: 'none'
                        })
                    }
                }
            }
        })
    },

    // 取消服务包
    handleCancelPackage(e) {
        const { id, name } = e.currentTarget.dataset
        console.log('[取消服务包] packageId:', id, ', name:', name)

        wx.showModal({
            title: '确认取消',
            content: `确定要取消「${name}」服务包吗？取消后将无法接该类型订单。`,
            confirmColor: '#ee0a24',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        wx.showLoading({ title: '取消中...', mask: true })
                        const result = await api.servicePackage.cancel(id)
                        console.log('[取消服务包] 成功:', result)
                        wx.hideLoading()

                        wx.showToast({
                            title: '已取消',
                            icon: 'success'
                        })

                        this.loadPackages(true)
                    } catch (err) {
                        wx.hideLoading()
                        console.error('[取消服务包] 失败:', err)
                        wx.showToast({
                            title: err.message || '取消失败',
                            icon: 'none'
                        })
                    }
                }
            }
        })
    }
})
