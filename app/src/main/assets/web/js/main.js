function humanFileSize(size) {
    if (size === 0) return '0 B';
    const i = Math.floor(Math.log(size) / Math.log(1024));
    return (size / Math.pow(1024, i)).toFixed(2) * 1 + ' ' + ['B', 'kB', 'MB', 'GB', 'TB'][i];
}

document.addEventListener('alpine:init', () => {
    Alpine.data('fileManager', (initialFiles, token) => ({
        view: 'grid',
        slideshow: false,
        files: initialFiles,
        sharedContent: [],
        sidebarOpen: false,
        hasNewSharedContent: false,
        sortKey: 'filename',
        sortAsc: true,
        copiedId: null,
        currentIndex: 0,

        get currentFile() {
            return this.sortedFiles[this.currentIndex];
        },

        get isGrid() {
            return !this.slideshow && this.view === 'grid';
        },

        get isList() {
            return !this.slideshow && this.view === 'list';
        },

        get isSlideshow() {
            return this.slideshow;
        },

        toggleSlideshow() {
            this.slideshow = !this.slideshow;
        },

        showList() {
            this.slideshow = false;
            this.view = 'list';
        },

        showGrid() {
            this.slideshow = false;
            this.view = 'grid';
        },

        toggleSidebar() {
            this.sidebarOpen = !this.sidebarOpen;
            if (this.sidebarOpen) {
                this.hasNewSharedContent = false;
            }
        },

        nextSlide() {
            this.currentIndex = (this.currentIndex + 1) % this.sortedFiles.length;
        },

        prevSlide() {
            this.currentIndex = (this.currentIndex - 1 + this.sortedFiles.length) % this.sortedFiles.length;
        },

        get sortedFiles() {
            return [...this.files].sort((a, b) => {
                let valA = a[this.sortKey];
                let valB = b[this.sortKey];
                if (typeof valA === 'string') {
                    valA = valA.toLowerCase();
                    valB = valB.toLowerCase();
                }
                if (valA < valB) return this.sortAsc ? -1 : 1;
                if (valA > valB) return this.sortAsc ? 1 : -1;
                return 0;
            });
        },

        copyLink(fileId) {
            const link = window.location.origin + '/' + token + '/file/' + fileId;

            const handleSuccess = () => {
                this.copiedId = fileId;
                setTimeout(() => { if(this.copiedId === fileId) this.copiedId = null; }, 2000);
            };

            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(link).then(handleSuccess);
            } else {
                const textArea = document.createElement('textarea');
                textArea.value = link;
                textArea.style.position = 'fixed';
                textArea.style.left = '-999999px';
                textArea.style.top = '-999999px';
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();
                try {
                    document.execCommand('copy');
                    handleSuccess();
                } catch (err) {
                    console.error('Fallback copy failed', err);
                }
                document.body.removeChild(textArea);
            }
        },

        copyToClipboard(text) {
            if (navigator.clipboard && navigator.clipboard.writeText) {
                navigator.clipboard.writeText(text);
            } else {
                const textArea = document.createElement('textarea');
                textArea.value = text;
                textArea.style.position = 'fixed';
                textArea.style.left = '-999999px';
                textArea.style.top = '-999999px';
                document.body.appendChild(textArea);
                textArea.focus();
                textArea.select();
                try {
                    document.execCommand('copy');
                } catch (err) {
                    console.error('Fallback copy failed', err);
                }
                document.body.removeChild(textArea);
            }
        },

        initSSE() {
            const eventSource = new EventSource('/' + token + '/events');

            eventSource.addEventListener('add', (event) => {
                const newFile = JSON.parse(event.data);
                if (!this.files.find(f => f.fileId === newFile.fileId)) {
                    this.files.push(newFile);
                }
            });

            eventSource.addEventListener('remove', (event) => {
                const fileIdToRemove = event.data;
                this.files = this.files.filter(f => f.fileId !== fileIdToRemove);
                if (this.currentIndex >= this.files.length) {
                    this.currentIndex = this.files.length > 0 ? this.files.length - 1 : 0;
                }
                if (this.files.length === 0) {
                    this.currentIndex = 0;
                    this.slideshow = false;
                }
            });

            eventSource.addEventListener('addSharedContent', (event) => {
                const newContent = JSON.parse(event.data);
                this.sharedContent.push(newContent);
                console.log(this.sharedContent)
                if (!this.sidebarOpen) {
                    this.hasNewSharedContent = true;
                }
            });

            eventSource.addEventListener('removeSharedContent', (event) => {
                const contentIdToRemove = event.data;
                this.sharedContent = this.sharedContent.filter(f => f.id !== contentIdToRemove);
            });

            eventSource.addEventListener('init', (event) => {
                this.files = [];
                console.log("Filelist cleared");
            })

            eventSource.onerror = (err) => {
                console.error('SSE failed, reconnecting...', err);
            };
        }
    }));
});
