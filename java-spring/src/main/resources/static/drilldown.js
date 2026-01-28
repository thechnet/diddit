  function addCredentials(form) {
    form.username.value = localStorage.getItem("username");
    form.password.value = localStorage.getItem("password");
  }

  function interact(target, endpoint) {
    const post_id = target.getAttribute('data-post_id');
    const username = localStorage.getItem("username");
    const password = localStorage.getItem("password");
    fetch(endpoint, {
        method: 'POST',
        headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
        body: `post_id=${encodeURIComponent(post_id)}&username=${encodeURIComponent(username)}&password=${encodeURIComponent(password)}`
    }).then(response => response.text()).then(x => {});
  }

  function likePost(event) {
    interact(event.currentTarget, '/tasks/like');
    event.stopPropagation();
  }

  function dislikePost(event) {
    interact(event.currentTarget, '/tasks/dislike');
    event.stopPropagation();
  }

  function deletePost(event) {
    interact(event.currentTarget, '/tasks/delete');
    event.stopPropagation();
  }

  function drillInto(event) {
    window.location.href = `/posts?parent=${event.currentTarget.getAttribute('data-_id')}`;
    event.stopPropagation();
  }
