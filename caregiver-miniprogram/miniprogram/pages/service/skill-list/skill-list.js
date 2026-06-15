const api = require('../../../utils/api')

const SKILL_CATEGORIES = [
    { id: 0, name: '全部' },
    { id: 1, name: '临床医疗护理' },
    { id: 2, name: '基础生活照料' },
    { id: 3, name: '康复训练与介护' },
    { id: 4, name: '失智专项护理' },
    { id: 5, name: '居家安全与应急' },
    { id: 6, name: '精神慰藉与社交' }
]

Page({
    data: {
        categories: SKILL_CATEGORIES,
        activeCategory: 0,
        keyword: '',
        skillList: [],
        filteredList: [],
        loading: false
    },

    onLoad() {
        this.loadData()
    },

    onShow() {
        this.loadData()
    },

    // 调用 /skill/all 接口，后端直接返回带申请状态的技能列表
    async loadData() {
        if (this.data.loading) return

        this.setData({ loading: true })

        try {
            const { activeCategory } = this.data
            const skillType = activeCategory > 0 ? activeCategory : undefined

            const skillList = await api.skill.getAllWithStatus(skillType) || []

            this.setData({
                skillList,
                loading: false
            })
            this.filterByKeyword()
        } catch (err) {
            console.error('加载技能列表失败:', err)
            this.setData({ loading: false })
        }
    },

    // 按关键词前端过滤
    filterByKeyword() {
        const { skillList, keyword } = this.data
        if (!keyword) {
            this.setData({ filteredList: skillList })
            return
        }
        const filtered = skillList.filter(item =>
            item.skillName && item.skillName.indexOf(keyword) >= 0
        )
        this.setData({ filteredList: filtered })
    },

    // 搜索
    onSearchChange(e) {
        this.setData({ keyword: e.detail })
        this.filterByKeyword()
    },

    onSearch() {
        this.filterByKeyword()
    },

    onClearSearch() {
        this.setData({ keyword: '' })
        this.filterByKeyword()
    },

    // 切换分类
    onCategoryChange(e) {
        const id = parseInt(e.currentTarget.dataset.id)
        this.setData({ activeCategory: id, keyword: '' })
        this.loadData()
    },

    // 下拉刷新
    onPullDownRefresh() {
        this.loadData()
        setTimeout(() => {
            wx.stopPullDownRefresh()
        }, 1000)
    },

    // 申请技能
    handleApplySkill(e) {
        const { id, name, needAudit } = e.currentTarget.dataset

        if (needAudit === 1) {
            wx.showModal({
                title: '申请技能',
                content: `确定要申请「${name}」吗？该技能需要审核。`,
                confirmText: '提交申请',
                success: async (res) => {
                    if (res.confirm) {
                        await this.doApplySkill(id)
                    }
                }
            })
        } else {
            this.doApplySkill(id)
        }
    },

    // 执行申请技能
    async doApplySkill(skillId) {
        try {
            wx.showLoading({ title: '申请中...', mask: true })
            await api.skill.add({ skillId })
            wx.hideLoading()

            wx.showToast({
                title: '申请成功',
                icon: 'success'
            })

            this.loadData()
        } catch (err) {
            wx.hideLoading()
            console.error('申请技能失败:', err)
        }
    },

    // 删除技能
    onDeleteSkill(e) {
        const { id, name } = e.currentTarget.dataset

        wx.showModal({
            title: '确认删除',
            content: `确定要删除技能「${name}」吗？`,
            success: async (res) => {
                if (res.confirm) {
                    try {
                        wx.showLoading({ title: '删除中...', mask: true })
                        await api.skill.delete(id)
                        wx.hideLoading()

                        wx.showToast({
                            title: '删除成功',
                            icon: 'success'
                        })

                        this.loadData()
                    } catch (err) {
                        wx.hideLoading()
                        console.error('删除技能失败:', err)
                    }
                }
            }
        })
    }
})
