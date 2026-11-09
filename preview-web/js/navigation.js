/**
 * LifeFlow Web Preview - Navigation Controller
 * Manages screen routing and bottom navigation state across 5 destinations.
 */
const Navigation = (() => {
    let currentTab = 'home';

    function init() {
        const profile = Storage.getProfile();
        if (profile.onboardingCompleted) {
            showMainScreen();
        } else {
            showOnboardingScreen();
        }

        setupBottomBarListeners();
    }

    function showOnboardingScreen() {
        const onboardingEl = document.getElementById('screen-onboarding');
        const mainEl = document.getElementById('screen-main');
        if (onboardingEl) onboardingEl.classList.remove('hidden');
        if (mainEl) mainEl.classList.add('hidden');
    }

    function showMainScreen() {
        const onboardingEl = document.getElementById('screen-onboarding');
        const mainEl = document.getElementById('screen-main');
        if (onboardingEl) onboardingEl.classList.add('hidden');
        if (mainEl) mainEl.classList.remove('hidden');
        goToTab(currentTab);
    }

    function goToTab(tabName) {
        currentTab = tabName;
        const tabs = ['home', 'activities', 'planning', 'habits', 'settings'];

        tabs.forEach(tab => {
            const tabContent = document.getElementById(`tab-content-${tab}`);
            const tabBtn = document.querySelector(`.bottom-nav-item[data-tab="${tab}"]`);

            if (tabContent) {
                if (tab === tabName) {
                    tabContent.classList.remove('hidden');
                } else {
                    tabContent.classList.add('hidden');
                }
            }

            if (tabBtn) {
                if (tab === tabName) {
                    tabBtn.classList.add('active');
                } else {
                    tabBtn.classList.remove('active');
                }
            }
        });

        // Trigger updates when tab becomes active
        if (window.App) {
            if (tabName === 'home') App.refreshHome();
            if (tabName === 'activities') App.refreshActivities();
            if (tabName === 'planning') App.refreshPlanning();
            if (tabName === 'habits') App.refreshHabits();
            if (tabName === 'settings') App.refreshSettings();
        }
    }

    function setupBottomBarListeners() {
        const buttons = document.querySelectorAll('.bottom-nav-item');
        buttons.forEach(btn => {
            btn.addEventListener('click', () => {
                const tab = btn.getAttribute('data-tab');
                if (tab) goToTab(tab);
            });
        });
    }

    return {
        init,
        showOnboardingScreen,
        showMainScreen,
        goToTab,
        getCurrentTab: () => currentTab
    };
})();
