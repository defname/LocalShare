// SPDX-FileCopyrightText: 2026 2026 defname
//
// SPDX-License-Identifier: GPL-3.0-or-later

function humanFileSize(size) {
    if (size === 0) return '0 B';
    const i = Math.floor(Math.log(size) / Math.log(1024));
    return (size / Math.pow(1024, i)).toFixed(2) * 1 + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
}

// ─── LightGallery Integration ─────────────────────────────────────────────────
let lgInstance = null;

function buildGalleryItems(files, token) {
    return files.map(file => {
        const isVideo = file.mimeType && file.mimeType.startsWith('video/');
        const isImage = file.mimeType && file.mimeType.startsWith('image/');
        const src = '/' + token + '/file/' + file.fileId;
        const thumb = (isImage || isVideo) ? '/' + token + '/thumbnail/' + file.fileId : '/' + token + '/icon/' + file.icon;

        if (isVideo) {
            return {
                src: src,
                thumb: thumb,
                subHtml: '<h4>' + file.filename + '</h4><p>' + humanFileSize(file.size) + '</p>',
                video: { source: [{ src: src, type: file.mimeType }], attributes: { preload: false, controls: true } }
            };
        }
        return {
            src: isImage ? src : '/' + token + '/icon/' + file.icon,
            thumb: thumb,
            downloadUrl: src + '?download',
            subHtml: '<h4>' + file.filename + '</h4><p>' + humanFileSize(file.size) + '</p>'
        };
    });
}

function openLightGallery(files, token, startIndex) {
    const el = document.getElementById('lg-inline-container');
    if (!el) return;

    if (lgInstance) {
        lgInstance.destroy();
        lgInstance = null;
    }

    lgInstance = lightGallery(el, {
        dynamic: true,
        dynamicEl: buildGalleryItems(files, token),
        index: startIndex || 0,
        plugins: [lgZoom, lgThumbnail, lgVideo],
        speed: 400,
        thumbnail: true,
        animateThumb: true,
        zoomFromOrigin: false,
        allowMediaOverlap: true,
        toggleThumb: true,
        download: true,
        mobileSettings: {
            controls: true,
            showCloseIcon: true,
            download: true
        }
    });

    lgInstance.openGallery(startIndex || 0);
}

// ─── Alpine Component ─────────────────────────────────────────────────────────

document.addEventListener('alpine:init', () => {
    Alpine.data('fileManager', (initialFiles, token) => ({
        view: 'grid',
        files: initialFiles,
        sharedContent: [],
        sidebarOpen: false,
        hasNewSharedContent: false,
        sortKey: 'filename',
        sortAsc: true,
        copiedId: null,

        get sortedFiles() {
            return [...this.files].sort((a, b) => {
                let valA = a[this.sortKey];
                let valB = b[this.sortKey];
                if (typeof valA === 'string') { valA = valA.toLowerCase(); valB = valB.toLowerCase(); }
                if (valA < valB) return this.sortAsc ? -1 : 1;
                if (valA > valB) return this.sortAsc ? 1 : -1;
                return 0;
            });
        },

        get isGrid() { return this.view === 'grid'; },
        get isList() { return this.view === 'list'; },

        showList() { this.view = 'list'; },
        showGrid() { this.view = 'grid'; },

        toggleSidebar() {
            this.sidebarOpen = !this.sidebarOpen;
            if (this.sidebarOpen) this.hasNewSharedContent = false;
        },

        openPreview(index) {
            openLightGallery(this.sortedFiles, token, index);
        },

        canPreview(file) {
            return file.mimeType && (
                file.mimeType.startsWith('image/') ||
                file.mimeType.startsWith('video/')
            );
        },

        loadThumbnail(el, file) {
            if (!file) return;
            const canThumb = file.hasThumbnail ||
                (file.mimeType && (file.mimeType.startsWith('image/') || file.mimeType.startsWith('video/')));
            if (!canThumb) return;

            const thumbUrl = '/' + token + '/thumbnail/' + file.fileId;
            const img = new Image();
            img.onload = () => {
                el.src = thumbUrl;
                el.classList.remove('p-2', 'w-20', 'h-20', 'w-8', 'h-8');
                el.classList.add('object-cover', 'w-full', 'h-full');
            };
            img.src = thumbUrl;
        },

        copyLink(fileId) {
            const link = window.location.origin + '/' + token + '/file/' + fileId;
            const handleSuccess = () => {
                this.copiedId = fileId;
                setTimeout(() => { if (this.copiedId === fileId) this.copiedId = null; }, 2000);
            };
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(link).then(handleSuccess);
            } else {
                const ta = document.createElement('textarea');
                ta.value = link;
                ta.style.position = 'fixed';
                ta.style.left = '-999999px';
                document.body.appendChild(ta);
                ta.focus(); ta.select();
                try { document.execCommand('copy'); handleSuccess(); } catch (e) {}
                document.body.removeChild(ta);
            }
        },

        copyToClipboard(text) {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text);
            } else {
                const ta = document.createElement('textarea');
                ta.value = text;
                ta.style.position = 'fixed';
                ta.style.left = '-999999px';
                document.body.appendChild(ta);
                ta.focus(); ta.select();
                try { document.execCommand('copy'); } catch (e) {}
                document.body.removeChild(ta);
            }
        },

        doneSharing() {
            if (!confirm('End this sharing session? Your access will be removed.')) return;
            fetch('/' + token + '/session', { method: 'DELETE' })
                .then(() => { window.location.href = '/' + token + '/'; })
                .catch(() => { window.location.href = '/' + token + '/'; });
        },

        initSSE() {
            const connect = () => {
                const es = new EventSource('/' + token + '/events');

                es.addEventListener('init', (event) => {
                    // Server sends current full list on (re)connect — restore it
                    try {
                        this.files = JSON.parse(event.data);
                    } catch (e) {
                        this.files = [];
                    }
                });

                es.addEventListener('add', (event) => {
                    const newFile = JSON.parse(event.data);
                    if (!this.files.find(f => f.fileId === newFile.fileId)) {
                        this.files.push(newFile);
                    }
                });

                es.addEventListener('remove', (event) => {
                    const id = event.data;
                    this.files = this.files.filter(f => f.fileId !== id);
                });

                es.addEventListener('addSharedContent', (event) => {
                    const newContent = JSON.parse(event.data);
                    this.sharedContent.push(newContent);
                    if (!this.sidebarOpen) this.hasNewSharedContent = true;
                });

                es.addEventListener('removeSharedContent', (event) => {
                    const id = event.data;
                    this.sharedContent = this.sharedContent.filter(f => f.id !== id);
                });

                es.onerror = () => {
                    es.close();
                    setTimeout(connect, 3000);
                };
            };
            connect();
        }
    }));
});
