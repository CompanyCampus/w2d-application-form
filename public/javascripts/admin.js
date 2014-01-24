var Admin = {
  init: function() {
    if(window.location.pathname.indexOf("/records/") == 0) {
      $("input,textarea").attr("disabled", "disabled");
    }
    if(window.location.pathname == "/records") {
      $("tbody tr").click(function(e) {
        window.location = "/records/" + e.currentTarget.dataset.uuid;
      });
    }

    $(".delete").click(function(e) {
      e.preventDefault();
      var btn = $(this);
      if(btn.hasClass("btn-danger")) {
        $.ajax({
          type: "DELETE",
          url: "/users/" + btn.data("target"),
          success: function() {
            btn.parents("tr").fadeOut();
          },
          error: function() {
            alert(I18N["user.delete.fail"]);
          }
        });
      } else {
        btn.addClass("btn-danger");
        btn.text($(this).data("text-confirm"));
      }
    });
    $(".delete").mouseout(function() {
      $(this).removeClass("btn-danger");
      $(this).text($(this).data("text-normal"));
    })
  }
};
