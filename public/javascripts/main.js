var Main = {
  init: function() {
    $("tr").click(function(e) {
      window.location = "/records/" + e.currentTarget.dataset.uuid;
    });
    $(".more .show").click(function(e) {
      e.preventDefault();
      $(".more .show").hide();
      $(".more .content").show();
    });
  }
};
