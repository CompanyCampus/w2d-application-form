var Main = {
  init: function() {
    $("tbody tr").click(function(e) {
      window.location = "/records/" + e.currentTarget.dataset.uuid;
    });
    $("#companyCreation").datepicker({
      dateFormat: "yy-mm-dd",
      changeMonth: true,
      changeYear: true,
      maxDate: 0,
      minDate: "-10Y"
    });
    $("#pitch").keyup(function() {
      $(".pitch-chars-count").text(400 - $(this).val().length);
    })
    if(window.location.pathname.indexOf("/records/") == 0) {
      $("input,textarea").attr("disabled", "disabled");
    }
    if(window.location.pathname == "/records") {
      $("#error-modal").modal("show");
    }

    if($("form").hasClass("alreadysubmitted")) {
      $("input, textarea").attr("disabled", "disabled");
    }

    $(".apply").click(function(e) {
      e.preventDefault();
      $("form.submission").trigger("submit", { submit: true });
    });

    $("form.submission").submit(function(e, data) {
      if(typeof data == "undefined" || data.submit == false) {
        e.preventDefault();
        $.ajax({
          type: "PUT",
          url: "/submission",
          data: $("form.submission").serialize(),
          success: function(data) {
            noty({text: I18N.savesuccess, type: "success", timeout: 3000});
          },
          error: function(data) {
            if(data.status == 403) {
              noty({text: I18N.alreadysubmitted, type: "error", timeout: 5000});
            } else {
              noty({text: I18N.savefailed, type: "error", timeout: 5000});
            }
          }
        });
      }
    })
  }
};
