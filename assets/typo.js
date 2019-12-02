
function onCtrlEnter(e) {
    if (event.ctrlKey && event.keyCode === 13) {
        sendTypo();
    }
}


function sendTypo() {

    var br = "%0D%0A";
    var br2 = br + br;
    var cite = "> ";

    var selection = window.getSelection();
    var quote = selection.toString();

    if (quote == "") {
        return;
    }

    var par = selection.anchorNode.textContent;
    var url = window.location.href;
    var title = document.title;

    selection.empty();

    var to = "ivan@grishaev.me";
    var subject = "Опечатка в публикации " + title;
    var content = "Привет, Иван!" + br2 +
        "У тебя в блоге опечатка:"
        + br2 + cite + quote + br2 +
        "Параграф целиком:" + br2 + cite + par
        + br2 + url;

    var a = document.createElement("a");
    a.href = "mailto:ivan@grishaev.me?subject=Опечатка&body=У тебя в блоге опечатка";
    a.href = "mailto:" + to + "?subject=" + subject + "&body=" + content;
    document.body.appendChild(a);
    a.click();

}

document.onkeyup = onCtrlEnter;
