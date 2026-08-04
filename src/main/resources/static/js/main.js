document.addEventListener('turbo:load', event => {
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
    const navLinks = document.querySelectorAll('#sidebar-nav .sidebar-link');
    
    // First remove active class from all
    navLinks.forEach(link => link.classList.remove('active'));
    
    // Find the best match
    let bestMatch = null;
    let maxMatchLength = -1;
    
    navLinks.forEach(link => {
        const path = link.getAttribute('data-path');
        if (!path) return; // Skip links without a data-path attribute
        
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
    }
});
