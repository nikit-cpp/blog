import Vue from 'vue'

export default (MediumEditor) => {
    var CLASS_DRAG_OVER = 'medium-editor-dragover';
    function clearClassNames(element) {
        var editable = MediumEditor.util.getContainerEditorElement(element),
            existing = Array.prototype.slice.call(editable.parentElement.querySelectorAll('.' + CLASS_DRAG_OVER));

        existing.forEach(function (el) {
            el.classList.remove(CLASS_DRAG_OVER);
        });
    }

    return MediumEditor.Extension.extend({
            name: 'image-upload',
            allowedTypes: ['image'],

            init: function () {
                console.log("init image-upload extension");
                MediumEditor.Extension.prototype.init.apply(this, arguments);

                this.subscribe('editableDrag', this.handleDrag.bind(this));
                this.subscribe('editableDrop', this.handleDrop.bind(this));
            },

            handleDrag: function (event) {
                console.log('handle drag');
                event.preventDefault();
                event.dataTransfer.dropEffect = 'copy';

                var target = event.target.classList ? event.target : event.target.parentElement;

                // Ensure the class gets removed from anything that had it before
                clearClassNames(target);

                if (event.type === 'dragover') {
                    target.classList.add(CLASS_DRAG_OVER);
                }
            },

            handleDrop: function (event) {
                console.log('handle drop');
                // Prevent file from opening in the current window
                event.preventDefault();
                event.stopPropagation();
                // Select the dropping target, and set the selection to the end of the target
                // https://github.com/yabwe/medium-editor/issues/980
                this.base.selectElement(event.target);
                var selection = this.base.exportSelection();
                selection.start = selection.end;
                this.base.importSelection(selection);
                // IE9 does not support the File API, so prevent file from opening in the window
                // but also don't try to actually get the file
                if (event.dataTransfer.files) {
                    Array.prototype.slice.call(event.dataTransfer.files).forEach(function (file) {
                        if (this.isAllowedFile(file)) {
                            if (file.type.match('image')) {
                                this.insertImageFile(file);
                            }
                        }
                    }, this);
                }

                // Make sure we remove our class from everything
                clearClassNames(event.target);
            },

            isAllowedFile: function (file) {
                return this.allowedTypes.some(function (fileType) {
                    return !!file.type.match(fileType);
                });
            },

            insertImageFile: function (file) {
                console.log("insertImageFile");

                const formData = new FormData();
                formData.append('image', file);

                Vue.http.post('/api/image/post/content', formData)
                    .then((result) => {
                        const addImageElement = this.document.createElement('img');

                        const url = result.data.relativeUrl; // Get url from response
                        console.log("got url", url);
                        addImageElement.src = url;

                        MediumEditor.util.insertHTMLCommand(this.document, addImageElement.outerHTML);
                    })
                    .catch((err) => {
                        console.log(err);
                    })

            }
        });
};