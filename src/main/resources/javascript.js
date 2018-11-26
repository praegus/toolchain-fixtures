function openImage(imgEl) {
    var myImage = new Image;
    var imageSrc = imgEl.getAttribute("src");
    myImage.src = imageSrc;
        myImage.style.border = 'none';
        myImage.style.outline = 'none';
        myImage.style.position = 'fixed';
        myImage.style.left = '0';
        myImage.style.top = '0';

    var newWindow = window.open("", "image");
        newWindow.document.write(myImage.outerHTML);
}
