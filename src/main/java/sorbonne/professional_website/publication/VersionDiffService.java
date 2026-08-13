package sorbonne.professional_website.publication;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sorbonne.professional_website.entity.WebsiteVersion;
import sorbonne.professional_website.exception.ResourceNotFoundException;
import sorbonne.professional_website.mapper.WebsiteVersionMapper;
import sorbonne.professional_website.repository.WebsiteVersionRepository;

import java.util.*;

@Service
public class VersionDiffService {
    private final WebsiteVersionRepository repository;
    private final ObjectMapper objectMapper;
    public VersionDiffService(WebsiteVersionRepository repository, ObjectMapper objectMapper) { this.repository = repository; this.objectMapper = objectMapper; }

    @Transactional(readOnly = true)
    public VersionDiffResponse diff(Long ownerId, Long fromId, Long toId) {
        JsonNode from = objectMapper.valueToTree(WebsiteVersionMapper.toResponse(find(ownerId, fromId)));
        JsonNode to = objectMapper.valueToTree(WebsiteVersionMapper.toResponse(find(ownerId, toId)));
        List<VersionDiffEntry> changes = new ArrayList<>();
        compare("", from, to, changes);
        return new VersionDiffResponse(fromId, toId, changes.size(), changes);
    }

    private WebsiteVersion find(Long ownerId, Long id) { return repository.findByIdAndOwnerOwnerId(id, ownerId).orElseThrow(() -> new ResourceNotFoundException("Website version not found: " + id)); }

    private void compare(String path, JsonNode a, JsonNode b, List<VersionDiffEntry> out) {
        if (Objects.equals(a, b)) return;
        if (a != null && b != null && a.isObject() && b.isObject()) {
            Set<String> names = new TreeSet<>(); a.fieldNames().forEachRemaining(names::add); b.fieldNames().forEachRemaining(names::add);
            for (String name : names) compare(path + "/" + name, a.get(name), b.get(name), out);
            return;
        }
        if (a != null && b != null && a.isArray() && b.isArray()) {
            int size = Math.max(a.size(), b.size());
            for (int i=0;i<size;i++) compare(path + "/" + i, i<a.size()?a.get(i):null, i<b.size()?b.get(i):null, out);
            return;
        }
        out.add(new VersionDiffEntry(path.isEmpty()?"/":path, stringify(a), stringify(b)));
    }
    private String stringify(JsonNode n) { return n == null || n.isMissingNode() ? null : n.toString(); }
}
