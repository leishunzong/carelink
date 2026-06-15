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

const CATEGORY_MAP = {
    1: '居家陪护', 2: '医院陪护', 3: '周期护理',
    4: '家政服务', 5: '陪诊服务', 6: '母婴护理'
}

Page({
    data: {
        categories: PACKAGE_CATEGORIES,
        activeCategory: 0,
        allPackages: [],
        packageList: [],
        loading: false
    },

    onLoad() {
        this.loadPackages()
    },

    onShow() {
        this.loadPackages()
    },

    // 调用 /caregiver/package/my 接口，查询我开通的服务包列表
    async loadPackages() {
        if (this.data.loading) return

        this.setData({ loading: true })

        try {
            const allPackages = (await api.servicePackage.getMyPackages()) || []

            // 补充分类显示文案
            allPackages.forEach(item => {
                item.categoryText = CATEGORY_MAP[item.category] || '未知类型'
            })

            this.setData({ allPackages, loading: false })
            this.filterByCategory()
        } catch (err) {
            console.error('加载服务包列表失败:', err)
            this.setData({ loading: false })
        }
    },

    // 按分类前端过滤
    filterByCategory() {
        const { allPackages, activeCategory } = this.data
        const packageList = activeCategory > 0
            ? allPackages.filter(item => item.category === activeCategory)
            : allPackages

        this.setData({ packageList })
    },

    // 切换分类
    onCategoryChange(e) {
        const id = parseInt(e.currentTarget.dataset.id)
        this.setData({ activeCategory: id })
        this.filterByCategory()
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.loadPackages()
        setTimeout(() => {
            wx.stopPullDownRefresh()
        }, 1000)
    },

    // 取消服务包
    handleCancelPackage(e) {
        const { id, name } = e.currentTarget.dataset

        wx.showModal({
            title: '确认取消',
            content: `确定要取消「${name}」服务包吗？取消后将无法接该类型订单。`,
            confirmColor: '#ee0a24',
            success: async (res) => {
                if (res.confirm) {
                    try {
                        wx.showLoading({ title: '取消中...', mask: true })
                        await api.servicePackage.cancel(id)
                        wx.hideLoading()

                        wx.showToast({
                            title: '已取消',
                            icon: 'success'
                        })

                        this.loadPackages()
                    } catch (err) {
                        wx.hideLoading()
                        console.error('取消服务包失败:', err)
                    }
                }
            }
        })
    },

    // 跳转到开通服务包页
    goToOpenPackage() {
        wx.navigateTo({
            url: '/pages/service/package-list/package-list'
        })
    }
})
