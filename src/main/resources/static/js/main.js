window.addEventListener('DOMContentLoaded', event => {
    // Toggle the side navigation
    const sidebarToggle = document.body.querySelector('#menu-toggle');
    if (sidebarToggle) {
        sidebarToggle.addEventListener('click', event => {
            event.preventDefault();
            document.body.classList.toggle('sb-sidenav-toggled');
        });
    }

    // Set active sidebar link based on current path
    const currentPath = window.location.pathname;
    const navLinks = document.querySelectorAll('#sidebar-nav .list-group-item');
    
    // First remove active class from all
    navLinks.forEach(link => link.classList.remove('active'));
    
    // Find the best match
    let bestMatch = null;
    let maxMatchLength = -1;
    
    navLinks.forEach(link => {
        const path = link.getAttribute('data-path');
        if (path === '/' && currentPath === '/') {
            bestMatch = link;
            maxMatchLength = 1;
        } else if (path !== '/' && currentPath.startsWith(path) && path.length > maxMatchLength) {
            bestMatch = link;
            maxMatchLength = path.length;
        }
    });
    
    if (bestMatch) {
        bestMatch.classList.add('active');
        // Optional: add a visual indicator like a custom class for styling
        bestMatch.style.backgroundColor = 'rgba(255,255,255,0.1)';
        bestMatch.style.color = '#fff';
    }
});
