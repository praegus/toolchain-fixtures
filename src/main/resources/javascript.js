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

function toggleScenario(scenarioEl) {
    if(scenarioEl.nodeName.toLowerCase() == 'td') {
        scenarioEl = scenarioEl.parentElement;
    }
    var currentClass = scenarioEl.nextElementSibling.getAttribute("class");
    var details = scenarioEl.nextElementSibling;
    if (details.getAttribute('class').includes('closed-detail')) {
        details.setAttribute('class', 'scenario-detail');
    } else {
        details.setAttribute('class', 'scenario-detail closed-detail');
    }
}

function toggleCollapsible(collapsible) {
    if(collapsible.nodeName.toLowerCase() == 'p') {
        collapsible = collapsible.parentElement;
    }
    var currentClass = collapsible.getAttribute("class");
    if (currentClass.includes('closed')) {
        collapsible.setAttribute('class', 'collapsible');
    } else {
        collapsible.setAttribute('class', 'collapsible closed');
    }
}

function enableClickHandlers() {
    var scenarios = document.getElementsByClassName('scenario');
    var collapsibles = document.getElementsByClassName('collapsible');
    for(var i=0; i < scenarios.length; i++) {
        scenarios[i].addEventListener("click", function(event){
            var el = event.target;
            toggleScenario(el);
        });
    }
    for(var i=0; i < collapsibles.length; i++) {
        collapsibles[i].getElementsByTagName('p')[0].addEventListener("click", function(event){
            var el = event.target;
            toggleCollapsible(el);
        });
        }
}



