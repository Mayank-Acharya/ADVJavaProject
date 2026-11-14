// ====================================================================
// SmartATS Real-Time AJAX Notification Bell
// Polls backend every 10 seconds for unread recruitment alerts
// ====================================================================

document.addEventListener('DOMContentLoaded', function () {
    const bellBtn = document.getElementById('bellBtn');
    const bellBadge = document.getElementById('bellBadge');
    const bellDropdown = document.getElementById('bellDropdown');
    const notifList = document.getElementById('notifList');
    const markReadBtn = document.getElementById('markReadBtn');

    if (!bellBtn) return;

    let isDropdownOpen = false;

    // Check for unread alerts immediately and every 10 seconds
    fetchUnreadCount();
    setInterval(fetchUnreadCount, 10000);

    // Toggle Notification Dropdown
    bellBtn.addEventListener('click', function (e) {
        e.stopPropagation();
        isDropdownOpen = !isDropdownOpen;
        if (isDropdownOpen) {
            bellDropdown.style.display = 'block';
            fetchRecentNotifications();
        } else {
            bellDropdown.style.display = 'none';
        }
    });

    // Close dropdown on outside click
    document.addEventListener('click', function (e) {
        if (isDropdownOpen && !bellDropdown.contains(e.target) && e.target !== bellBtn) {
            bellDropdown.style.display = 'none';
            isDropdownOpen = false;
        }
    });

    // Mark all as read
    if (markReadBtn) {
        markReadBtn.addEventListener('click', function (e) {
            e.stopPropagation();
            fetch('notifications?action=markRead', { method: 'POST' })
                .then(res => res.json())
                .then(data => {
                    if (data.success) {
                        bellBadge.style.display = 'none';
                        bellBadge.innerText = '0';
                        fetchRecentNotifications();
                    }
                })
                .catch(err => console.error('Error marking notifications as read:', err));
        });
    }

    function fetchUnreadCount() {
        fetch('notifications?action=unreadCount')
            .then(res => res.json())
            .then(data => {
                if (data.unreadCount && data.unreadCount > 0) {
                    bellBadge.innerText = data.unreadCount > 99 ? '99+' : data.unreadCount;
                    bellBadge.style.display = 'flex';
                } else {
                    bellBadge.style.display = 'none';
                }
            })
            .catch(err => console.debug('Notification poll waiting:', err));
    }

    function fetchRecentNotifications() {
        if (!notifList) return;
        notifList.innerHTML = '<div style="padding: 16px; text-align: center; color: #94a3b8; font-size: 12px;">Loading alerts...</div>';

        fetch('notifications?action=recent')
            .then(res => res.json())
            .then(data => {
                if (!data.notifications || data.notifications.length === 0) {
                    notifList.innerHTML = '<div style="padding: 20px; text-align: center; color: #94a3b8; font-size: 13px;">No notifications yet.</div>';
                    return;
                }

                let html = '';
                data.notifications.forEach(n => {
                    const unreadClass = n.isRead ? '' : 'unread';
                    html += `
                        <div class="notif-item ${unreadClass}">
                            <div class="notif-title">${escapeHtml(n.title)}</div>
                            <div class="notif-msg">${escapeHtml(n.message)}</div>
                            <div class="notif-time">${escapeHtml(n.date)}</div>
                        </div>
                    `;
                });
                notifList.innerHTML = html;
            })
            .catch(err => {
                notifList.innerHTML = '<div style="padding: 16px; color: #ef4444; font-size: 12px;">Failed to load alerts.</div>';
                console.error(err);
            });
    }

    function escapeHtml(str) {
        if (!str) return '';
        return str.replace(/&/g, '&amp;')
                  .replace(/</g, '&lt;')
                  .replace(/>/g, '&gt;')
                  .replace(/"/g, '&quot;')
                  .replace(/'/g, '&#039;');
    }
});
